package com.jing.whaletv.data.repository

import androidx.room.withTransaction
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.local.ChannelEntity
import com.jing.whaletv.data.local.ProgramEntity
import com.jing.whaletv.data.local.StreamEntity
import com.jing.whaletv.data.local.SyncStateEntity
import com.jing.whaletv.data.local.WhaleTvDatabase
import com.jing.whaletv.data.local.toDomain
import com.jing.whaletv.data.model.ParsedChannel
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SettingsTestResult
import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.data.network.FetchResult
import com.jing.whaletv.data.network.PlaylistClient
import com.jing.whaletv.data.parser.M3uParser
import com.jing.whaletv.data.parser.XmltvParser
import java.net.URI
import java.io.StringReader
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ChannelRepository(
    private val database: WhaleTvDatabase,
    private val playlistClient: PlaylistClient,
    private val m3uParser: M3uParser = M3uParser(),
    private val xmltvParser: XmltvParser = XmltvParser(),
) {
    private val channelDao = database.channelDao()
    private val programDao = database.programDao()
    private val syncStateDao = database.syncStateDao()
    private val playlistSyncMutex = Mutex()
    private val startupProbeClient = OkHttpClient.Builder()
        .connectTimeout(STARTUP_STREAM_PRECHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(STARTUP_STREAM_PRECHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .callTimeout(STARTUP_STREAM_PRECHECK_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun observeChannels(): Flow<List<TvChannel>> {
        return combine(
            channelDao.observePlayableChannelsWithStreams(
                healthyStatus = StreamHealth.HEALTHY.name,
                unknownStatus = StreamHealth.UNKNOWN.name,
            ),
            programDao.observeAllPrograms(),
        ) { channels, programs ->
            val now = System.currentTimeMillis()
            val programsByChannel = programs.groupBy { it.channelId }
            channels.map { channel ->
                channel.toDomain(now, programsByChannel[channel.channel.id].orEmpty())
            }
        }.flowOn(Dispatchers.Default)
    }

    fun observeSyncSummary(): Flow<SyncSummary> {
        return syncStateDao.observeAll().map { states ->
            val values = states.associate { it.key to it.value }
            SyncSummary(
                playlistLastAttemptAt = values[KEY_PLAYLIST_ATTEMPT]?.toLongOrNull(),
                playlistLastSuccessAt = values[KEY_PLAYLIST_SUCCESS]?.toLongOrNull(),
                playlistLastError = values[KEY_PLAYLIST_ERROR],
                epgLastAttemptAt = values[KEY_EPG_ATTEMPT]?.toLongOrNull(),
                epgLastSuccessAt = values[KEY_EPG_SUCCESS]?.toLongOrNull(),
                epgLastError = values[KEY_EPG_ERROR],
                discoveredEpgUrl = values[KEY_DISCOVERED_EPG_URL],
            )
        }
    }

    fun observeSettingsDiagnostics(): Flow<SettingsDiagnostics> {
        return combine(
            channelDao.observeChannelCount(),
            channelDao.observePlayableChannelCount(
                healthyStatus = StreamHealth.HEALTHY.name,
                unknownStatus = StreamHealth.UNKNOWN.name,
            ),
            channelDao.observeStreamCount(),
            programDao.observeProgramCount(),
            channelDao.observeFavoriteCount(),
            channelDao.observeHistoryCount(),
            channelDao.observeUnhealthyStreamCount(StreamHealth.UNHEALTHY.name),
        ) { values ->
            SettingsDiagnostics(
                channelCount = values[0],
                playableChannelCount = values[1],
                streamCount = values[2],
                programCount = values[3],
                favoriteCount = values[4],
                historyCount = values[5],
                unhealthyStreamCount = values[6],
            )
        }.flowOn(Dispatchers.Default)
    }

    suspend fun hasCachedPlayableChannels(): Boolean {
        return channelDao.countPlayableChannels(
            healthyStatus = StreamHealth.HEALTHY.name,
            unknownStatus = StreamHealth.UNKNOWN.name,
        ) > 0
    }

    suspend fun setChannelFavorite(channelId: String, isFavorite: Boolean) = withContext(Dispatchers.Default) {
        channelDao.setChannelFavorite(channelId, isFavorite)
    }

    suspend fun markPlaybackReady(channelId: String, streamUrl: String) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            channelDao.updateStreamSuccess(
                channelId = channelId,
                url = streamUrl,
                healthStatus = StreamHealth.HEALTHY.name,
                successAt = now,
            )
            channelDao.setChannelLastWatchedAt(channelId, now)
            channelDao.setChannelAvailability(channelId, true)
        }
    }

    suspend fun markPlaybackFailed(channelId: String, streamUrl: String) = withContext(Dispatchers.Default) {
        database.withTransaction {
            val current = channelDao.getStream(channelId, streamUrl) ?: return@withTransaction
            channelDao.updateStreamFailure(
                channelId = channelId,
                url = streamUrl,
                healthStatus = StreamHealth.UNKNOWN.name,
                failureCount = current.failureCount + 1,
                failedAt = System.currentTimeMillis(),
            )
        }
    }

    suspend fun testDefaultPlaylistSource(): SettingsTestResult {
        return testPlaylistUrl(AppConstants.PRIMARY_PLAYLIST_URL)
    }

    suspend fun testActiveEpgSource(): SettingsTestResult {
        val xmltvUrl = withContext(Dispatchers.Default) {
            syncStateDao.getValue(KEY_DISCOVERED_EPG_URL).orEmpty()
        }
        if (xmltvUrl.isBlank()) {
            return SettingsTestResult(false, "尚未发现节目单地址，请先立即刷新")
        }
        return testEpgUrl(xmltvUrl)
    }

    private suspend fun testPlaylistUrl(url: String): SettingsTestResult = withContext(Dispatchers.Default) {
        val normalizedUrl = url.trim()
        if (!isHttpUrl(normalizedUrl)) {
            return@withContext SettingsTestResult(false, "请输入 http/https M3U 地址")
        }

        runCatching {
            val result = playlistClient.fetchText(url = normalizedUrl)
            val body = (result as FetchResult.Success).body
            val channels = m3uParser.parse(body)
            if (channels.isEmpty()) {
                SettingsTestResult(false, "源可访问，但没有解析到频道")
            } else {
                val streamCount = channels.sumOf { it.streams.size }
                val epgHint = m3uParser.parseXmltvUrl(body)
                    ?.let { "，发现 EPG" }
                    .orEmpty()
                SettingsTestResult(true, "源可用：${channels.size} 个频道，$streamCount 个播放源$epgHint")
            }
        }.getOrElse { error ->
            SettingsTestResult(false, "测试源失败：${error.userFacingMessage()}")
        }
    }

    private suspend fun testEpgUrl(url: String): SettingsTestResult = withContext(Dispatchers.Default) {
        val normalizedUrl = url.trim()
        if (!isHttpUrl(normalizedUrl)) {
            return@withContext SettingsTestResult(false, "请输入 http/https XMLTV 地址")
        }

        runCatching {
            val result = playlistClient.fetchText(url = normalizedUrl)
            val body = (result as FetchResult.Success).body
            val knownChannelIds = channelDao.getAllChannels().map { it.id }.toSet()
            val allowedChannelIds = knownChannelIds.ifEmpty { sampleXmltvChannelIds(body) }
            val programs = xmltvParser.parse(StringReader(body), allowedChannelIds)
            when {
                allowedChannelIds.isEmpty() -> SettingsTestResult(false, "节目单可访问，但没有找到节目频道")
                programs.isEmpty() -> SettingsTestResult(false, "节目单可解析，但没有匹配当前频道")
                else -> SettingsTestResult(true, "节目单可用：解析到 ${programs.size} 条节目")
            }
        }.getOrElse { error ->
            SettingsTestResult(false, "测试节目单失败：${error.userFacingMessage()}")
        }
    }

    suspend fun resetStreamHealth() = withContext(Dispatchers.Default) {
        database.withTransaction {
            channelDao.resetAllStreamHealth(StreamHealth.UNKNOWN.name)
            channelDao.markChannelsWithStreamsAvailable()
        }
    }

    suspend fun clearEpgCache() = withContext(Dispatchers.Default) {
        programDao.deleteAllPrograms()
    }

    suspend fun clearWatchHistory() = withContext(Dispatchers.Default) {
        channelDao.clearWatchHistory()
    }

    suspend fun syncPlaylists() = withContext(Dispatchers.Default) {
        playlistSyncMutex.withLock {
            setState(KEY_PLAYLIST_ATTEMPT, System.currentTimeMillis().toString())
            val sources = listOf(Source("primary", AppConstants.PRIMARY_PLAYLIST_URL))

            val parsed = mutableListOf<ParsedChannel>()
            var notModifiedCount = 0
            var successCount = 0
            val hasCachedChannels = channelDao.countPlayableChannels(
                healthyStatus = StreamHealth.HEALTHY.name,
                unknownStatus = StreamHealth.UNKNOWN.name,
            ) > 0

            try {
                sources.forEach { source ->
                    when (val result = fetchSource(source, useCacheHeaders = hasCachedChannels)) {
                        FetchResult.NotModified -> notModifiedCount += 1
                        is FetchResult.Success -> {
                            successCount += 1
                            saveHttpCacheHeaders(source.cacheKey(), result)
                            m3uParser.parseXmltvUrl(result.body)?.let { xmltvUrl ->
                                setState(KEY_DISCOVERED_EPG_URL, xmltvUrl)
                            }
                            parsed += m3uParser.parse(result.body)
                        }
                    }
                }

                if (parsed.isNotEmpty()) {
                    importParsedChannels(
                        channels = parsed,
                        markMissingUnavailable = notModifiedCount == 0,
                    )
                }

                if (successCount > 0 || notModifiedCount > 0) {
                    restorePlaylistPlayableState()
                    setState(KEY_PLAYLIST_SUCCESS, System.currentTimeMillis().toString())
                    setState(KEY_PLAYLIST_ERROR, null)
                }
            } catch (error: Throwable) {
                setState(KEY_PLAYLIST_ERROR, error.message ?: error::class.java.simpleName)
                throw error
            }
        }
    }

    suspend fun syncEpg() = withContext(Dispatchers.Default) {
        setState(KEY_EPG_ATTEMPT, System.currentTimeMillis().toString())
        val xmltvUrl = syncStateDao.getValue(KEY_DISCOVERED_EPG_URL).orEmpty()
        if (xmltvUrl.isBlank()) {
            setState(KEY_EPG_ERROR, null)
            return@withContext
        }

        try {
            val channelIds = channelDao.getAllChannels().map { it.id }.toSet()
            if (channelIds.isEmpty()) {
                setState(KEY_EPG_ERROR, null)
                return@withContext
            }

            val result = playlistClient.fetchText(
                url = xmltvUrl,
                etag = syncStateDao.getValue("$KEY_EPG.etag"),
                lastModified = syncStateDao.getValue("$KEY_EPG.last_modified"),
            )

            if (result is FetchResult.NotModified) {
                setState(KEY_EPG_SUCCESS, System.currentTimeMillis().toString())
                setState(KEY_EPG_ERROR, null)
                return@withContext
            }

            val success = result as FetchResult.Success
            setState("$KEY_EPG.etag", success.etag)
            setState("$KEY_EPG.last_modified", success.lastModified)

            val now = System.currentTimeMillis()
            val programs = xmltvParser
                .parse(StringReader(success.body), channelIds)
                .filter { it.endAt > now - ONE_HOUR_MS }
                .map { it.toEntity() }

            database.withTransaction {
                programDao.deleteProgramsEndedBefore(now - ONE_HOUR_MS)
                programDao.upsertPrograms(programs)
            }
            setState(KEY_EPG_SUCCESS, now.toString())
            setState(KEY_EPG_ERROR, null)
        } catch (error: Throwable) {
            setState(KEY_EPG_ERROR, error.message ?: error::class.java.simpleName)
            throw error
        }
    }

    suspend fun precheckStartupStreams() {
        val streams = channelDao.getStartupProbeStreams(
            healthyStatus = StreamHealth.HEALTHY.name,
            unknownStatus = StreamHealth.UNKNOWN.name,
            limit = STARTUP_STREAM_PRECHECK_LIMIT,
        )
        if (streams.isEmpty()) return

        coroutineScope {
            val probeSlots = Semaphore(STARTUP_STREAM_PRECHECK_CONCURRENCY)
            streams.forEach { streamEntity ->
                launch(Dispatchers.IO) {
                    val stream = streamEntity.toDomain()
                    probeSlots.withPermit {
                        probeStartupStream(stream)
                    }
                }
            }
        }
    }

    private suspend fun probeStartupStream(stream: TvStream) {
        if (!isProbeSupported(stream.url)) return
        if (probeStreamReachable(stream)) {
            markStreamSucceeded(stream)
        } else {
            markStreamStartupProbeFailed(stream)
        }
    }

    private suspend fun markStreamStartupProbeFailed(stream: TvStream) {
        database.withTransaction {
            val currentEntity = channelDao.getStream(stream.channelId, stream.url)
            val nextFailureCount = ((currentEntity?.failureCount ?: stream.failureCount) + 1).coerceAtLeast(1)
            val now = System.currentTimeMillis()
            if (currentEntity == null) {
                channelDao.upsertStreams(
                    listOf(
                        StreamEntity(
                            channelId = stream.channelId,
                            url = stream.url,
                            quality = stream.quality,
                            label = stream.label,
                            referrer = stream.referrer,
                            userAgent = stream.userAgent,
                            healthStatus = StreamHealth.UNKNOWN.name,
                            failureCount = nextFailureCount,
                            lastFailureAt = now,
                            sortOrder = stream.sortOrder,
                        ),
                    ),
                )
            } else {
                channelDao.updateStreamFailure(
                    channelId = stream.channelId,
                    url = stream.url,
                    healthStatus = StreamHealth.UNKNOWN.name,
                    failureCount = nextFailureCount,
                    failedAt = now,
                )
            }
        }
    }

    private suspend fun probeStreamReachable(stream: TvStream): Boolean {
        return runCatching {
            withContext(Dispatchers.IO) {
                startupProbeClient.newCall(buildProbeRequest(stream)).execute().use { response ->
                    response.isSuccessful || response.code in 300..399
                }
            }
        }.getOrDefault(false)
    }

    private fun buildProbeRequest(stream: TvStream): Request {
        val requestBuilder = Request.Builder()
            .url(stream.url)
            .get()
            .header("Range", "bytes=0-0")
            .header("User-Agent", stream.userAgent ?: "WhaleTV/1.0 AndroidTV")
        stream.referrer?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Referer", it) }
        return requestBuilder.build()
    }

    private fun isProbeSupported(url: String): Boolean {
        return isHttpUrl(url)
    }

    private fun isHttpUrl(url: String): Boolean {
        return runCatching {
            URI(url).scheme?.lowercase()
        }.getOrDefault(null) in setOf("http", "https")
    }

    private fun sampleXmltvChannelIds(content: String): Set<String> {
        return xmltvChannelRegex.findAll(content)
            .map { it.groupValues[1].substringBefore("@").ifBlank { it.groupValues[1] } }
            .take(XMLTV_TEST_CHANNEL_SAMPLE_LIMIT)
            .toSet()
    }

    private suspend fun markStreamSucceeded(stream: TvStream) {
        database.withTransaction {
            channelDao.updateStreamSuccess(
                channelId = stream.channelId,
                url = stream.url,
                healthStatus = StreamHealth.HEALTHY.name,
                successAt = System.currentTimeMillis(),
            )
            channelDao.setChannelAvailability(stream.channelId, true)
        }
    }

    private suspend fun fetchSource(source: Source, useCacheHeaders: Boolean = true): FetchResult {
        val cacheKey = source.cacheKey()
        return playlistClient.fetchText(
            url = source.url,
            etag = syncStateDao.getValue("$cacheKey.etag").takeIf { useCacheHeaders },
            lastModified = syncStateDao.getValue("$cacheKey.last_modified").takeIf { useCacheHeaders },
        )
    }

    private suspend fun saveHttpCacheHeaders(sourceKey: String, result: FetchResult.Success) {
        setState("$sourceKey.etag", result.etag)
        setState("$sourceKey.last_modified", result.lastModified)
    }

    private suspend fun importParsedChannels(channels: List<ParsedChannel>, markMissingUnavailable: Boolean) {
        val now = System.currentTimeMillis()
        val distinctChannels = channels
            .groupBy { it.id }
            .map { (_, variants) ->
                val first = variants.minWith(compareBy<ParsedChannel> { it.priority }.thenBy { it.name })
                first.copy(streams = variants.flatMap { it.streams }.distinctBy { it.url })
            }

        val existingChannels = channelDao.getAllChannels().associateBy { it.id }
        val existingStreams = channelDao.getAllStreams().associateBy { it.channelId to it.url }
        val channelEntities = distinctChannels.map { parsed ->
            val old = existingChannels[parsed.id]
            val hasPotentiallyPlayableStream = parsed.streams.isNotEmpty()
            ChannelEntity(
                id = parsed.id,
                name = parsed.name,
                logoUrl = parsed.logoUrl,
                groupTitle = parsed.groupTitle,
                priority = parsed.priority,
                isFavorite = old?.isFavorite ?: false,
                lastWatchedAt = old?.lastWatchedAt,
                isAvailable = hasPotentiallyPlayableStream,
                updatedAt = now,
            )
        }
        val streamEntities = distinctChannels.flatMap { parsed ->
            parsed.streams.mapIndexed { index, stream ->
                val old = existingStreams[stream.channelId to stream.url]
                StreamEntity(
                    channelId = stream.channelId,
                    url = stream.url,
                    quality = stream.quality,
                    label = stream.label,
                    referrer = stream.referrer,
                    userAgent = stream.userAgent,
                    healthStatus = old?.healthStatus
                        ?.takeUnless { it == StreamHealth.UNHEALTHY.name }
                        ?: StreamHealth.UNKNOWN.name,
                    failureCount = old?.failureCount ?: 0,
                    lastFailureAt = old?.lastFailureAt,
                    lastSuccessAt = old?.lastSuccessAt,
                    sortOrder = index,
                )
            }
        }
        val freshIds = distinctChannels.map { it.id }
        val freshIdSet = freshIds.toSet()
        val missingIds = if (markMissingUnavailable) {
            existingChannels.keys.filterNot { it in freshIdSet }
        } else {
            emptyList()
        }

        database.withTransaction {
            missingIds.chunked(SQLITE_BIND_PARAMETER_BATCH_SIZE).forEach { chunk ->
                channelDao.deleteStreamsForChannels(chunk)
                channelDao.deleteChannels(chunk)
            }
            channelDao.upsertChannels(channelEntities)
            freshIds.chunked(SQLITE_BIND_PARAMETER_BATCH_SIZE).forEach { chunk ->
                channelDao.deleteStreamsForChannels(chunk)
            }
            channelDao.upsertStreams(streamEntities)
        }
    }

    private suspend fun restorePlaylistPlayableState() {
        database.withTransaction {
            channelDao.resetUnhealthyStreams(
                unhealthyStatus = StreamHealth.UNHEALTHY.name,
                unknownStatus = StreamHealth.UNKNOWN.name,
            )
            channelDao.markChannelsWithStreamsAvailable()
        }
    }

    private suspend fun setState(key: String, value: String?) {
        syncStateDao.setValue(SyncStateEntity(key = key, value = value))
    }

    private data class Source(val key: String, val url: String) {
        fun cacheKey(): String = "$key.${url.hashCode().toUInt().toString(16)}"
    }

    private companion object {
        const val KEY_PLAYLIST_SUCCESS = "playlist.last_success_at"
        const val KEY_PLAYLIST_ATTEMPT = "playlist.last_attempt_at"
        const val KEY_PLAYLIST_ERROR = "playlist.last_error"
        const val KEY_EPG = "epg"
        const val KEY_EPG_ATTEMPT = "epg.last_attempt_at"
        const val KEY_EPG_SUCCESS = "epg.last_success_at"
        const val KEY_EPG_ERROR = "epg.last_error"
        const val KEY_DISCOVERED_EPG_URL = "epg.discovered_url"
        const val ONE_HOUR_MS = 60 * 60 * 1000L
        const val STARTUP_STREAM_PRECHECK_TIMEOUT_MS = 1_000L
        const val STARTUP_STREAM_PRECHECK_CONCURRENCY = 4
        const val STARTUP_STREAM_PRECHECK_LIMIT = 80
        const val SQLITE_BIND_PARAMETER_BATCH_SIZE = 900
        const val XMLTV_TEST_CHANNEL_SAMPLE_LIMIT = 80
        val xmltvChannelRegex = Regex("""<programme\b[^>]*\bchannel=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    }
}

private fun Throwable.userFacingMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

private fun Program.toEntity(): ProgramEntity = ProgramEntity(
    channelId = channelId,
    title = title,
    startAt = startAt,
    endAt = endAt,
    description = description,
)
