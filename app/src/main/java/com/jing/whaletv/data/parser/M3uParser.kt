package com.jing.whaletv.data.parser

import com.jing.whaletv.data.model.ParsedChannel
import com.jing.whaletv.data.model.ParsedStream
import java.text.Normalizer
import java.util.Locale

class M3uParser {
    fun parseXmltvUrl(content: String): String? {
        val header = content.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("#EXTM3U", ignoreCase = true) }
            ?: return null
        return parseAttributes(header)["x-tvg-url"]
            ?.split(',', ';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
    }

    fun parse(content: String): List<ParsedChannel> {
        groupByChannel.clear()
        logoByChannel.clear()
        nameByChannel.clear()

        val streams = mutableListOf<ParsedStream>()
        var pendingInfo: PendingInfo? = null
        var pendingReferrer: String? = null
        var pendingUserAgent: String? = null

        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingInfo = parseInfo(line)
                    pendingReferrer = null
                    pendingUserAgent = null
                }
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val option = line.substringAfter(":")
                    val key = option.substringBefore("=", "").lowercase(Locale.US)
                    val value = option.substringAfter("=", "").trim().ifBlank { null }
                    when (key) {
                        "http-referrer", "http-referer" -> pendingReferrer = value
                        "http-user-agent" -> pendingUserAgent = value
                    }
                }
                line.isBlank() || line.startsWith("#") -> Unit
                pendingInfo != null -> {
                    val info = pendingInfo
                    streams += ParsedStream(
                        channelId = info.id,
                        url = line,
                        quality = info.quality,
                        label = info.label,
                        referrer = pendingReferrer,
                        userAgent = pendingUserAgent,
                        sortOrder = streams.count { it.channelId == info.id },
                    )
                    pendingInfo = null
                    pendingReferrer = null
                    pendingUserAgent = null
                }
            }
        }

        return streams
            .distinctBy { it.channelId to it.url }
            .groupBy { it.channelId }
            .map { (channelId, channelStreams) ->
                val first = channelStreams.first()
                ParsedChannel(
                    id = channelId,
                    name = first.label?.let { sanitizeName(it) }.takeUnless { it.isNullOrBlank() }
                        ?: channelNameFromId(channelId),
                    logoUrl = firstLabelLogo(channelId),
                    groupTitle = groupByChannel[channelId] ?: "Undefined",
                    priority = ChannelClassifier.priority(channelId, channelNameFromId(channelId), groupByChannel[channelId] ?: ""),
                    streams = channelStreams.mapIndexed { index, stream -> stream.copy(sortOrder = index) },
                )
            }
            .sortedWith(compareBy<ParsedChannel> { it.priority }.thenBy { it.name })
    }

    private val groupByChannel = mutableMapOf<String, String>()
    private val logoByChannel = mutableMapOf<String, String?>()
    private val nameByChannel = mutableMapOf<String, String>()

    private fun parseInfo(line: String): PendingInfo {
        val attrs = parseAttributes(line)
        val rawName = line.substringAfterLast(",", "").trim()
        val cleanName = sanitizeName(rawName)
        val rawId = attrs["tvg-id"].orEmpty()
        val id = normalizeChannelId(rawId.ifBlank { cleanName })
        val groupTitle = attrs["group-title"].orEmpty().substringBefore(";").ifBlank { "Undefined" }
        val logoUrl = attrs["tvg-logo"]?.ifBlank { null }
        val quality = qualityRegex.find(rawName)?.groupValues?.getOrNull(1)
        val label = cleanName.ifBlank { rawName }

        groupByChannel.putIfAbsent(id, groupTitle)
        logoByChannel.putIfAbsent(id, logoUrl)
        nameByChannel.putIfAbsent(id, cleanName.ifBlank { id })

        return PendingInfo(
            id = id,
            quality = quality,
            label = label,
        )
    }

    private fun parseAttributes(line: String): Map<String, String> {
        return attributeRegex.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun channelNameFromId(id: String): String = nameByChannel[id] ?: id.substringBefore(".")

    private fun firstLabelLogo(id: String): String? = logoByChannel[id]

    private fun normalizeChannelId(value: String): String {
        val withoutFeed = value.substringBefore("@").ifBlank { value }
        val normalized = Normalizer.normalize(withoutFeed, Normalizer.Form.NFKC)
        return normalized
            .replace(Regex("[^A-Za-z0-9._-]+"), ".")
            .trim('.')
            .ifBlank { "channel.${withoutFeed.hashCode().toUInt()}" }
    }

    private fun sanitizeName(value: String): String {
        return value
            .replace(qualityRegex, "")
            .replace(bracketLabelRegex, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class PendingInfo(
        val id: String,
        val quality: String?,
        val label: String,
    )

    private companion object {
        val attributeRegex = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")
        val qualityRegex = Regex("""\((4K|8K|\d{3,4}[pi])\)""", RegexOption.IGNORE_CASE)
        val bracketLabelRegex = Regex("""\[[^\]]+]""")
    }
}
