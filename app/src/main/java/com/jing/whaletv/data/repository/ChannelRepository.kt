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
import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SettingsTestResult
import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.data.model.streamHealthAfterFailure
import com.jing.whaletv.data.network.FetchResult
import com.jing.whaletv.data.network.PlaylistClient
import com.jing.whaletv.data.parser.M3uParser
import com.jing.whaletv.data.parser.XmltvParser
import java.io.StringReader
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val settingsRepository: SettingsRepository,
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
                fullPlaylistLastAttemptAt = values[KEY_FULL_PLAYLIST_ATTEMPT]?.toLongOrNull(),
                fullPlaylistLastSuccessAt = values[KEY_FULL_PLAYLIST_SUCCESS]?.toLongOrNull(),
                fullPlaylistLastError = values[KEY_FULL_PLAYLIST_ERROR],
                epgLastAttemptAt = values[KEY_EPG_ATTEMPT]?.toLongOrNull(),
                epgLastSuccessAt = values[KEY_EPG_SUCCESS]?.toLongOrNull(),
                epgLastError = values[KEY_EPG_ERROR],
                discoveredEpgUrl = values[KEY_DISCOVERED_EPG_URL],
                epgGuideSourceCount = values[KEY_EPG_GUIDE_SOURCE_COUNT]?.toIntOrNull() ?: 0,
                epgGuideSampleChannelIds = parseStoredList(values[KEY_EPG_GUIDE_SAMPLE_CHANNELS]),
            )
        }
    }

    fun observeSettingsDiagnostics(): Flow<SettingsDiagnostics> {
        val counts = combine(
            channelDao.observeChannelCount(),
            channelDao.observePlayableChannelCount(
                healthyStatus = StreamHealth.HEALTHY.name,
                unknownStatus = StreamHealth.UNKNOWN.name,
            ),
            channelDao.observeStreamCount(),
            programDao.observeProgramCount(),
            programDao.observeProgramChannelCount(),
            channelDao.observeFavoriteCount(),
            channelDao.observeHistoryCount(),
            channelDao.observeUnhealthyStreamCount(StreamHealth.UNHEALTHY.name),
        ) { values ->
            SettingsDiagnostics(
                channelCount = values[0],
                playableChannelCount = values[1],
                streamCount = values[2],
                programCount = values[3],
                epgChannelCount = values[4],
                favoriteCount = values[5],
                historyCount = values[6],
                unhealthyStreamCount = values[7],
            )
        }

        return combine(
            counts,
            programDao.observeProgramChannelSamples(EPG_SETTINGS_SAMPLE_CHANNEL_LIMIT),
        ) { diagnostics, sampleChannels ->
            diagnostics.copy(epgSampleChannelIds = sampleChannels)
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
            val nextFailureCount = current.failureCount + 1
            channelDao.updateStreamFailure(
                channelId = channelId,
                url = streamUrl,
                healthStatus = streamHealthAfterFailure(nextFailureCount).name,
                failureCount = nextFailureCount,
                failedAt = System.currentTimeMillis(),
            )
        }
    }

    suspend fun testDefaultPlaylistSource(): SettingsTestResult {
        val scope = settingsRepository.settings.first().playlistScope
        return testPlaylistUrls(playlistSourcesForScope(scope))
    }

    suspend fun testActiveEpgSource(): SettingsTestResult {
        val channelIds = withContext(Dispatchers.Default) {
            channelDao.getAvailableChannels().map { it.id }.toSet()
        }
        if (channelIds.isEmpty()) {
            return SettingsTestResult(false, "尚未同步频道，请先立即刷新")
        }
        val sources = resolveEpgSources(channelIds)
        if (sources.urls.isEmpty()) {
            return SettingsTestResult(false, "尚未发现可用节目单地址，请先立即刷新")
        }
        return testEpgUrls(sources.urls, channelIds)
    }

    private suspend fun testPlaylistUrls(sources: List<PlaylistSource>): SettingsTestResult = withContext(Dispatchers.Default) {
        var lastError: Throwable? = null
        sources.forEach { source ->
            val normalizedUrl = source.url.trim()
            if (!isHttpUrl(normalizedUrl)) {
                lastError = IllegalArgumentException("请输入 http/https M3U 地址")
                return@forEach
            }

            runCatching {
                val result = playlistClient.fetchText(url = normalizedUrl)
                val body = (result as FetchResult.Success).body
                val channels = m3uParser.parse(body)
                if (channels.isEmpty()) {
                    throw IllegalStateException("源可访问，但没有解析到频道")
                }
                val streamCount = channels.sumOf { it.streams.size }
                val epgHint = m3uParser.parseXmltvUrl(body)
                    ?.let { "，发现 EPG" }
                    .orEmpty()
                return@withContext SettingsTestResult(
                    true,
                    "${source.label}可用：${channels.size} 个频道，$streamCount 个播放源$epgHint",
                )
            }.onFailure { error ->
                lastError = error
            }
        }
        SettingsTestResult(false, "测试源失败：${lastError?.userFacingMessage() ?: "没有可用源"}")
    }

    private suspend fun testEpgUrls(urls: List<String>, allowedChannelIds: Set<String>): SettingsTestResult = withContext(Dispatchers.Default) {
        testEpgSourceUrls(
            urls = urls,
            allowedChannelIds = allowedChannelIds,
            sourceLimit = EPG_TEST_SOURCE_LIMIT,
            fetchText = { url -> playlistClient.fetchText(url = url) },
            xmltvParser = xmltvParser,
        )
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

    suspend fun shouldBackfillAllPlaylists(): Boolean {
        return shouldBackfillAllForScope(settingsRepository.settings.first().playlistScope)
    }

    suspend fun backfillAllPlaylistsIfNeeded(): Boolean {
        if (!shouldBackfillAllPlaylists()) return false
        syncAllPlaylists()
        return true
    }

    suspend fun syncPlaylists() {
        syncPlaylistSources(mode = PlaylistSyncMode.PRIORITY)
    }

    suspend fun syncAllPlaylists() {
        syncPlaylistSources(mode = PlaylistSyncMode.ALL_BACKFILL)
    }

    private suspend fun syncPlaylistSources(mode: PlaylistSyncMode) = withContext(Dispatchers.Default) {
        playlistSyncMutex.withLock {
            val playlistScope = settingsRepository.settings.first().playlistScope
            val stateKeys = playlistSyncStateKeys(mode)
            val sources = playlistSourcesForSync(playlistScope, mode)
            setState(stateKeys.attempt, System.currentTimeMillis().toString())

            val parsed = mutableListOf<ParsedChannel>()
            var notModifiedCount = 0
            var successCount = 0
            val hasCachedChannels = channelDao.countPlayableChannels(
                healthyStatus = StreamHealth.HEALTHY.name,
                unknownStatus = StreamHealth.UNKNOWN.name,
            ) > 0
            val useCacheHeaders = mode == PlaylistSyncMode.PRIORITY &&
                playlistScope == PlaylistScope.ALL &&
                hasCachedChannels

            try {
                var lastSourceError: Throwable? = null
                sources.forEach { source ->
                    runCatching {
                        when (val result = fetchSource(source, useCacheHeaders = useCacheHeaders)) {
                            FetchResult.NotModified -> notModifiedCount += 1
                            is FetchResult.Success -> {
                                val parsedChannels = m3uParser.parse(result.body)
                                if (parsedChannels.isEmpty()) {
                                    throw IllegalStateException("${source.label}没有解析到频道")
                                }
                                successCount += 1
                                saveHttpCacheHeaders(source.cacheKey(), result)
                                m3uParser.parseXmltvUrl(result.body)?.let { xmltvUrl ->
                                    setState(KEY_DISCOVERED_EPG_URL, xmltvUrl)
                                }
                                parsed += parsedChannels
                            }
                        }
                    }.onFailure { error ->
                        lastSourceError = error
                    }
                }

                if (successCount == 0 && notModifiedCount == 0) {
                    throw lastSourceError ?: IllegalStateException("No playlist source succeeded")
                }

                if (parsed.isNotEmpty()) {
                    importParsedChannels(
                        channels = parsed,
                        missingChannelHandling = missingChannelHandlingForSync(playlistScope, mode),
                    )
                }

                if (successCount > 0 || notModifiedCount > 0) {
                    if (mode == PlaylistSyncMode.ALL_BACKFILL || playlistScope == PlaylistScope.ALL) {
                        restorePlaylistPlayableState()
                    }
                    setState(stateKeys.success, System.currentTimeMillis().toString())
                    setState(stateKeys.error, null)
                }
            } catch (error: Throwable) {
                setState(stateKeys.error, error.message ?: error::class.java.simpleName)
                throw error
            }
        }
    }

    suspend fun syncEpg() = withContext(Dispatchers.Default) {
        setState(KEY_EPG_ATTEMPT, System.currentTimeMillis().toString())

        try {
            val channelIds = channelDao.getAvailableChannels().map { it.id }.toSet()
            if (channelIds.isEmpty()) {
                setState(KEY_EPG_ERROR, null)
                return@withContext
            }

            val sourceResolution = resolveEpgSources(channelIds)
            if (sourceResolution.urls.isEmpty()) {
                setState(KEY_EPG_ERROR, null)
                return@withContext
            }

            val now = System.currentTimeMillis()
            val hasCachedPrograms = programDao.countPrograms() > 0
            val fetchedPrograms = mutableListOf<Program>()
            var successfulSourceCount = 0
            var lastError: Throwable? = null

            sourceResolution.urls.forEach { url ->
                runCatching {
                    when (val result = fetchEpgSource(url, useCacheHeaders = hasCachedPrograms)) {
                        FetchResult.NotModified -> successfulSourceCount += 1
                        is FetchResult.Success -> {
                            successfulSourceCount += 1
                            saveEpgSourceCacheHeaders(url, result)
                            fetchedPrograms += xmltvParser.parse(StringReader(result.body), channelIds)
                        }
                    }
                }.onFailure { error ->
                    lastError = error
                }
            }

            if (successfulSourceCount == 0) {
                throw lastError ?: IllegalStateException("No EPG source succeeded")
            }

            val programs = fetchedPrograms
                .filter { it.endAt > now - ONE_HOUR_MS }
                .distinctBy { Triple(it.channelId, it.startAt, it.title) }
                .map { it.toEntity() }

            database.withTransaction {
                programDao.deleteProgramsEndedBefore(now - ONE_HOUR_MS)
                if (programs.isNotEmpty()) {
                    programDao.upsertPrograms(programs)
                }
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
                            healthStatus = streamHealthAfterFailure(nextFailureCount).name,
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
                    healthStatus = streamHealthAfterFailure(nextFailureCount).name,
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

    private suspend fun resolveEpgSources(channelIds: Set<String>): EpgSourceResolution {
        val playlistEpgUrl = syncStateDao.getValue(KEY_DISCOVERED_EPG_URL)
            ?.trim()
            ?.takeIf(::isHttpUrl)

        setState(KEY_EPG_GUIDE_SOURCE_COUNT, "0")
        setState(KEY_EPG_GUIDE_SAMPLE_CHANNELS, null)

        val urls = buildList {
            playlistEpgUrl?.let(::add)
        }
            .distinct()
            .take(EPG_TOTAL_SOURCE_FETCH_LIMIT)

        return EpgSourceResolution(urls = urls)
    }

    private suspend fun fetchEpgSource(url: String, useCacheHeaders: Boolean): FetchResult {
        val cacheKey = epgSourceCacheKey(url)
        return playlistClient.fetchText(
            url = url,
            etag = syncStateDao.getValue("$cacheKey.etag").takeIf { useCacheHeaders },
            lastModified = syncStateDao.getValue("$cacheKey.last_modified").takeIf { useCacheHeaders },
        )
    }

    private suspend fun saveEpgSourceCacheHeaders(url: String, result: FetchResult.Success) {
        saveHttpCacheHeaders(epgSourceCacheKey(url), result)
    }

    private fun epgSourceCacheKey(url: String): String {
        return "$KEY_EPG.${url.hashCode().toUInt().toString(16)}"
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

    private suspend fun fetchSource(source: PlaylistSource, useCacheHeaders: Boolean = true): FetchResult {
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

    private suspend fun importParsedChannels(channels: List<ParsedChannel>, missingChannelHandling: MissingChannelHandling) {
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
                    healthStatus = old?.healthStatus ?: StreamHealth.UNKNOWN.name,
                    failureCount = old?.failureCount ?: 0,
                    lastFailureAt = old?.lastFailureAt,
                    lastSuccessAt = old?.lastSuccessAt,
                    sortOrder = index,
                )
            }
        }
        val freshIds = distinctChannels.map { it.id }
        val freshIdSet = freshIds.toSet()
        val missingIds = if (missingChannelHandling != MissingChannelHandling.KEEP_AVAILABLE_STATE) {
            existingChannels.keys.filterNot { it in freshIdSet }
        } else {
            emptyList()
        }

        database.withTransaction {
            missingIds.chunked(SQLITE_BIND_PARAMETER_BATCH_SIZE).forEach { chunk ->
                if (missingChannelHandling == MissingChannelHandling.MARK_UNAVAILABLE_AND_DELETE_STREAMS) {
                    channelDao.deleteStreamsForChannels(chunk)
                }
                channelDao.markChannelsUnavailable(chunk)
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
            channelDao.markChannelsWithStreamsAvailable()
        }
    }

    private suspend fun setState(key: String, value: String?) {
        syncStateDao.setValue(SyncStateEntity(key = key, value = value))
    }

    private data class EpgSourceResolution(
        val urls: List<String>,
    )

    private data class PlaylistSyncStateKeys(
        val attempt: String,
        val success: String,
        val error: String,
    )

    private fun playlistSyncStateKeys(mode: PlaylistSyncMode): PlaylistSyncStateKeys {
        return when (mode) {
            PlaylistSyncMode.PRIORITY -> PlaylistSyncStateKeys(
                attempt = KEY_PLAYLIST_ATTEMPT,
                success = KEY_PLAYLIST_SUCCESS,
                error = KEY_PLAYLIST_ERROR,
            )
            PlaylistSyncMode.ALL_BACKFILL -> PlaylistSyncStateKeys(
                attempt = KEY_FULL_PLAYLIST_ATTEMPT,
                success = KEY_FULL_PLAYLIST_SUCCESS,
                error = KEY_FULL_PLAYLIST_ERROR,
            )
        }
    }

    private companion object {
        const val KEY_PLAYLIST_SUCCESS = "playlist.last_success_at"
        const val KEY_PLAYLIST_ATTEMPT = "playlist.last_attempt_at"
        const val KEY_PLAYLIST_ERROR = "playlist.last_error"
        const val KEY_FULL_PLAYLIST_SUCCESS = "playlist.full.last_success_at"
        const val KEY_FULL_PLAYLIST_ATTEMPT = "playlist.full.last_attempt_at"
        const val KEY_FULL_PLAYLIST_ERROR = "playlist.full.last_error"
        const val KEY_EPG = "epg"
        const val KEY_EPG_ATTEMPT = "epg.last_attempt_at"
        const val KEY_EPG_SUCCESS = "epg.last_success_at"
        const val KEY_EPG_ERROR = "epg.last_error"
        const val KEY_DISCOVERED_EPG_URL = "epg.discovered_url"
        const val KEY_EPG_GUIDE_SOURCE_COUNT = "epg.guide_source_count"
        const val KEY_EPG_GUIDE_SAMPLE_CHANNELS = "epg.guide_sample_channels"
        const val ONE_HOUR_MS = 60 * 60 * 1000L
        const val STARTUP_STREAM_PRECHECK_TIMEOUT_MS = 1_000L
        const val STARTUP_STREAM_PRECHECK_CONCURRENCY = 4
        const val STARTUP_STREAM_PRECHECK_LIMIT = 80
        const val SQLITE_BIND_PARAMETER_BATCH_SIZE = 900
        const val EPG_TOTAL_SOURCE_FETCH_LIMIT = 8
        const val EPG_TEST_SOURCE_LIMIT = 4
        const val EPG_SETTINGS_SAMPLE_CHANNEL_LIMIT = 6
    }
}

internal enum class PlaylistSyncMode {
    PRIORITY,
    ALL_BACKFILL,
}

internal enum class MissingChannelHandling {
    KEEP_AVAILABLE_STATE,
    MARK_UNAVAILABLE,
    MARK_UNAVAILABLE_AND_DELETE_STREAMS,
}

internal data class PlaylistSource(val key: String, val url: String, val label: String) {
    fun cacheKey(): String = "$key.${url.hashCode().toUInt().toString(16)}"
}

private fun parseStoredList(value: String?): List<String> {
    return value
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
}

internal fun PlaylistScope.cacheKey(): String = "scope.$id"

internal fun playlistUrlForScope(scope: PlaylistScope): String = scope.playlistUrl

internal fun playlistSourcesForScope(scope: PlaylistScope): List<PlaylistSource> {
    return AppConstants.playlistUrls(scope.playlistPath).map { remoteUrl ->
        PlaylistSource(
            key = scope.cacheKey(),
            url = remoteUrl.url,
            label = remoteUrl.label,
        )
    }
}

internal fun shouldBackfillAllForScope(scope: PlaylistScope): Boolean {
    return scope != PlaylistScope.ALL
}

internal fun playlistSourcesForSync(scope: PlaylistScope, mode: PlaylistSyncMode): List<PlaylistSource> {
    val sourceScope = when (mode) {
        PlaylistSyncMode.PRIORITY -> scope
        PlaylistSyncMode.ALL_BACKFILL -> PlaylistScope.ALL
    }
    return playlistSourcesForScope(sourceScope)
}

internal fun missingChannelHandlingForSync(scope: PlaylistScope, mode: PlaylistSyncMode): MissingChannelHandling {
    return when {
        mode == PlaylistSyncMode.ALL_BACKFILL -> MissingChannelHandling.MARK_UNAVAILABLE_AND_DELETE_STREAMS
        scope == PlaylistScope.ALL -> MissingChannelHandling.MARK_UNAVAILABLE_AND_DELETE_STREAMS
        else -> MissingChannelHandling.MARK_UNAVAILABLE
    }
}

private fun Throwable.userFacingMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

internal suspend fun testEpgSourceUrls(
    urls: List<String>,
    allowedChannelIds: Set<String>,
    sourceLimit: Int,
    fetchText: suspend (String) -> FetchResult,
    xmltvParser: XmltvParser = XmltvParser(),
): SettingsTestResult = withContext(Dispatchers.Default) {
    val testUrls = urls.take(sourceLimit)
    if (testUrls.isEmpty()) {
        return@withContext SettingsTestResult(false, "尚未发现可用节目单地址，请先立即刷新")
    }

    val programs = mutableListOf<Program>()
    var readableSourceCount = 0
    var failureCount = 0
    var lastError: Throwable? = null

    testUrls.forEach { url ->
        runCatching {
            when (val result = fetchText(url)) {
                FetchResult.NotModified -> emptyList()
                is FetchResult.Success -> {
                    readableSourceCount += 1
                    xmltvParser.parse(StringReader(result.body), allowedChannelIds)
                }
            }
        }.onSuccess { parsed ->
            programs += parsed
        }.onFailure { error ->
            failureCount += 1
            lastError = error
        }
    }

    val distinctPrograms = programs.distinctBy { Triple(it.channelId, it.startAt, it.title) }
    when {
        distinctPrograms.isNotEmpty() -> SettingsTestResult(
            true,
            "节目单可用：${distinctPrograms.map { it.channelId }.distinct().size} 个频道，${distinctPrograms.size} 条节目",
        )
        failureCount == testUrls.size -> SettingsTestResult(
            false,
            "测试节目单失败：${lastError?.userFacingMessage() ?: "未知错误"}",
        )
        readableSourceCount == 0 -> SettingsTestResult(false, "测试节目单失败：没有返回节目单内容")
        else -> SettingsTestResult(false, "节目单可解析，但没有匹配当前频道")
    }
}

private fun Program.toEntity(): ProgramEntity = ProgramEntity(
    channelId = channelId,
    title = title,
    startAt = startAt,
    endAt = endAt,
    description = description,
)
