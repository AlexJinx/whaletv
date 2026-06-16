package com.jing.whaletv.data.parser

import org.json.JSONArray

data class EpgGuideSource(
    val channelId: String,
    val url: String,
    val site: String?,
    val language: String?,
)

class EpgGuideSourceParser {
    fun parse(content: String, allowedChannelIds: Set<String>): List<EpgGuideSource> {
        if (allowedChannelIds.isEmpty()) return emptyList()

        val allowed = allowedChannelIds.map(::normalizeChannelId).toSet()
        val guides = JSONArray(content)
        val sources = mutableListOf<EpgGuideSource>()

        for (guideIndex in 0 until guides.length()) {
            val guide = guides.optJSONObject(guideIndex) ?: continue
            val channelId = normalizeChannelId(guide.optString("channel"))
            if (channelId.isBlank() || channelId !in allowed) continue

            val guideSources = guide.optJSONArray("sources") ?: continue
            for (sourceIndex in 0 until guideSources.length()) {
                val source = guideSources.optJSONObject(sourceIndex) ?: continue
                val url = source.optString("url").trim()
                val format = source.optString("format").trim()
                if (!isHttpUrl(url) || !isXmltvSource(format, url)) continue

                sources += EpgGuideSource(
                    channelId = channelId,
                    url = url,
                    site = guide.optString("site").ifBlank { null },
                    language = guide.optString("lang").ifBlank { null },
                )
            }
        }

        return sources.distinctBy { it.channelId to it.url }
    }

    private fun normalizeChannelId(value: String): String {
        return value.substringBefore("@").trim()
    }

    private fun isHttpUrl(value: String): Boolean {
        return value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
    }

    private fun isXmltvSource(format: String, url: String): Boolean {
        val normalizedFormat = format.lowercase()
        val normalizedUrl = url.lowercase()
        return normalizedFormat in setOf("xml", "gzip") ||
            normalizedUrl.endsWith(".xml") ||
            normalizedUrl.endsWith(".xml.gz")
    }
}
