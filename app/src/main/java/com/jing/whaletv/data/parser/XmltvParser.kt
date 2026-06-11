package com.jing.whaletv.data.parser

import com.jing.whaletv.data.model.Program
import java.io.Reader
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class XmltvParser {
    fun parse(reader: Reader, allowedChannelIds: Set<String>): List<Program> {
        if (allowedChannelIds.isEmpty()) return emptyList()

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(reader)

        val programs = mutableListOf<Program>()
        var current: MutableProgram? = null
        var textTarget: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            val channelId = normalizeChannelId(parser.getAttributeValue(null, "channel").orEmpty())
                            val start = parseTimestamp(parser.getAttributeValue(null, "start"))
                            val stop = parseTimestamp(parser.getAttributeValue(null, "stop"))
                            current = if (channelId in allowedChannelIds && start != null && stop != null && stop > start) {
                                MutableProgram(channelId = channelId, startAt = start, endAt = stop)
                            } else {
                                null
                            }
                        }
                        "title", "desc" -> textTarget = parser.name
                    }
                }
                XmlPullParser.TEXT -> {
                    val program = current
                    val text = parser.text?.trim().orEmpty()
                    if (program != null && text.isNotBlank()) {
                        when (textTarget) {
                            "title" -> program.title = text
                            "desc" -> program.description = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            val program = current
                            if (program != null && program.title.isNotBlank()) {
                                programs += Program(
                                    channelId = program.channelId,
                                    title = program.title,
                                    startAt = program.startAt,
                                    endAt = program.endAt,
                                    description = program.description,
                                )
                            }
                            current = null
                            textTarget = null
                        }
                        "title", "desc" -> textTarget = null
                    }
                }
            }
            parser.next()
        }

        return programs.distinctBy { Triple(it.channelId, it.startAt, it.title) }
    }

    private fun normalizeChannelId(value: String): String = value.substringBefore("@").ifBlank { value }

    private fun parseTimestamp(value: String?): Long? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            OffsetDateTime.parse(raw, xmltvFormatter).toInstant().toEpochMilli()
        }.getOrElse {
            runCatching {
                val compact = raw.take(14)
                OffsetDateTime.of(
                    compact.substring(0, 4).toInt(),
                    compact.substring(4, 6).toInt(),
                    compact.substring(6, 8).toInt(),
                    compact.substring(8, 10).toInt(),
                    compact.substring(10, 12).toInt(),
                    compact.substring(12, 14).toInt(),
                    0,
                    ZoneOffset.UTC,
                ).toInstant().toEpochMilli()
            }.getOrNull()
        }
    }

    private data class MutableProgram(
        val channelId: String,
        val startAt: Long,
        val endAt: Long,
        var title: String = "",
        var description: String? = null,
    )

    private companion object {
        val xmltvFormatter: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss Z", Locale.US)
    }
}
