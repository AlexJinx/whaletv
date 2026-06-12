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
import kotlinx.coroutines.flow.first
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
    private val settingsRepository: SettingsRepository,
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
                playlistLastSuccessAt = values[KEY_PLAYLIST_SUCCESS]?.toLongOrNull(),
                playlistLastError = values[KEY_PLAYLIST_ERROR],
                epgLastSuccessAt = values[KEY_EPG_SUCCESS]?.toLongOrNull(),
                epgLastError = values[KEY_EPG_ERROR],
            )
        }
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

    suspend fun syncPlaylists() = withContext(Dispatchers.Default) {
        playlistSyncMutex.withLock {
            val settings = settingsRepository.settings.first()
            val sources = buildList {
                add(Source("primary", AppConstants.PRIMARY_PLAYLIST_URL))
                if (settings.customPlaylistUrl.isNotBlank()) add(Source("custom", settings.customPlaylistUrl))
            }

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
        val settings = settingsRepository.settings.first()
        val xmltvUrl = settings.xmltvUrl.ifBlank {
            syncStateDao.getValue(KEY_DISCOVERED_EPG_URL).orEmpty()
        }
        if (xmltvUrl.isBlank()) return@withContext

        try {
            val channelIds = channelDao.getAllChannels().map { it.id }.toSet()
            if (channelIds.isEmpty()) return@withContext

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
        return runCatching {
            URI(url).scheme?.lowercase()
        }.getOrDefault(null) in setOf("http", "https")
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
        const val KEY_PLAYLIST_ERROR = "playlist.last_error"
        const val KEY_EPG = "epg"
        const val KEY_EPG_SUCCESS = "epg.last_success_at"
        const val KEY_EPG_ERROR = "epg.last_error"
        const val KEY_DISCOVERED_EPG_URL = "epg.discovered_url"
        const val ONE_HOUR_MS = 60 * 60 * 1000L
        const val STARTUP_STREAM_PRECHECK_TIMEOUT_MS = 1_000L
        const val STARTUP_STREAM_PRECHECK_CONCURRENCY = 4
        const val STARTUP_STREAM_PRECHECK_LIMIT = 80
        const val SQLITE_BIND_PARAMETER_BATCH_SIZE = 900
    }
}

private fun Program.toEntity(): ProgramEntity = ProgramEntity(
    channelId = channelId,
    title = title,
    startAt = startAt,
    endAt = endAt,
    description = description,
)
