package com.jing.whaletv.data.repository

import com.jing.whaletv.data.parser.EpgGuideSource

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

internal const val EPG_GUIDE_SOURCE_FETCH_LIMIT = 6
internal const val EPG_GUIDE_SOURCE_CACHE_LIMIT = 80
