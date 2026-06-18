package com.jing.whaletv.data.repository

import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.core.RemoteUrl
import com.jing.whaletv.data.network.FetchResult
import com.jing.whaletv.data.parser.EpgGuideSource
import com.jing.whaletv.data.parser.EpgGuideSourceParser

internal class EpgGuideSourceDiscovery(
    private val fetchText: suspend (url: String, etag: String?, lastModified: String?) -> FetchResult,
    private val readState: suspend (String) -> String?,
    private val writeState: suspend (String, String?) -> Unit,
    private val parser: EpgGuideSourceParser = EpgGuideSourceParser(),
    private val guideApiUrls: List<RemoteUrl> = AppConstants.guidesApiUrls(),
    private val cacheLimit: Int = EPG_GUIDE_SOURCE_CACHE_LIMIT,
) {
    suspend fun discover(channelIds: Set<String>): List<EpgGuideSource> {
        if (channelIds.isEmpty()) return emptyList()

        val cached = readCachedGuideSources(channelIds)
        return runCatching {
            when (val response = fetchGuides(useCacheHeaders = true)) {
                NotModifiedGuideFetch -> cached.ifEmpty {
                    runCatching { fetchFreshGuideSources(channelIds) }.getOrElse { cached }
                }
                is GuideFetchSuccess -> parseAndCache(response, channelIds)
            }
        }.getOrElse {
            cached
        }
    }

    private suspend fun fetchFreshGuideSources(channelIds: Set<String>): List<EpgGuideSource> {
        return when (val response = fetchGuides(useCacheHeaders = false)) {
            NotModifiedGuideFetch -> emptyList()
            is GuideFetchSuccess -> parseAndCache(response, channelIds)
        }
    }

    private suspend fun fetchGuides(useCacheHeaders: Boolean): GuideFetchResult {
        var lastError: Throwable? = null
        guideApiUrls.forEach { source ->
            val cacheKey = guideApiCacheKey(source.url)
            runCatching {
                when (val result = fetchText(
                    source.url,
                    readState("$cacheKey.etag").takeIf { useCacheHeaders },
                    readState("$cacheKey.last_modified").takeIf { useCacheHeaders },
                )) {
                    FetchResult.NotModified -> return NotModifiedGuideFetch
                    is FetchResult.Success -> return GuideFetchSuccess(cacheKey = cacheKey, result = result)
                }
            }.onFailure { error ->
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException("No guide API source succeeded")
    }

    private suspend fun parseAndCache(response: GuideFetchSuccess, channelIds: Set<String>): List<EpgGuideSource> {
        writeState("${response.cacheKey}.etag", response.result.etag)
        writeState("${response.cacheKey}.last_modified", response.result.lastModified)
        val parsed = parser.parse(response.result.body, channelIds)
        saveCachedGuideSources(parsed)
        return parsed
    }

    private suspend fun readCachedGuideSources(channelIds: Set<String>): List<EpgGuideSource> {
        return parseCachedGuideSources(
            value = readState(EPG_GUIDE_SOURCE_CACHE_STATE_KEY),
            allowedChannelIds = channelIds,
        )
    }

    private suspend fun saveCachedGuideSources(sources: List<EpgGuideSource>) {
        val existing = parseCachedGuideSources(
            value = readState(EPG_GUIDE_SOURCE_CACHE_STATE_KEY),
            allowedChannelIds = null,
        )
        writeState(
            EPG_GUIDE_SOURCE_CACHE_STATE_KEY,
            serializeCachedGuideSources(sources + existing, cacheLimit),
        )
    }
}

private sealed interface GuideFetchResult

private data object NotModifiedGuideFetch : GuideFetchResult

private data class GuideFetchSuccess(
    val cacheKey: String,
    val result: FetchResult.Success,
) : GuideFetchResult

private fun guideApiCacheKey(url: String): String {
    return "$EPG_GUIDES_STATE_KEY.${url.hashCode().toUInt().toString(16)}"
}

internal fun selectGuideSourcesForFetch(sources: List<EpgGuideSource>, limit: Int): List<EpgGuideSource> {
    return sources
        .sortedWith(compareBy<EpgGuideSource> { it.channelId }.thenBy { epgSourceUrlPriority(it.url) }.thenBy { it.url })
        .distinctBy { it.url }
        .take(limit)
}

internal fun serializeCachedGuideSources(sources: List<EpgGuideSource>, limit: Int): String? {
    return sources
        .distinctBy { it.channelId to it.url }
        .take(limit)
        .joinToString("\n") { "${it.channelId}\t${it.url}" }
        .ifBlank { null }
}

internal fun parseCachedGuideSources(value: String?, allowedChannelIds: Set<String>?): List<EpgGuideSource> {
    val allowed = allowedChannelIds
        ?.map(::normalizeGuideChannelId)
        ?.toSet()
    return value
        ?.lineSequence()
        ?.mapNotNull { line ->
            val channelId = normalizeGuideChannelId(line.substringBefore('\t').trim())
            val url = line.substringAfter('\t', "").trim()
            if (channelId.isNotBlank() && isHttpGuideUrl(url) && (allowed == null || channelId in allowed)) {
                EpgGuideSource(channelId = channelId, url = url, site = null, language = null)
            } else {
                null
            }
        }
        ?.distinctBy { it.channelId to it.url }
        ?.toList()
        .orEmpty()
}

private fun normalizeGuideChannelId(value: String): String {
    return value.substringBefore("@").trim()
}

private fun isHttpGuideUrl(value: String): Boolean {
    return value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true)
}

private fun epgSourceUrlPriority(url: String): Int {
    return when {
        url.endsWith(".xml.gz", ignoreCase = true) -> 0
        url.endsWith(".xml", ignoreCase = true) -> 1
        else -> 2
    }
}

internal const val EPG_GUIDES_STATE_KEY = "epg.guides"
internal const val EPG_GUIDE_SOURCE_CACHE_STATE_KEY = "epg.guide_source_cache"
internal const val EPG_GUIDE_SOURCE_FETCH_LIMIT = 6
internal const val EPG_GUIDE_SOURCE_CACHE_LIMIT = 80
