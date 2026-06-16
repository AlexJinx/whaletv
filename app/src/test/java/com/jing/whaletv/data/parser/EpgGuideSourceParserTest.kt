package com.jing.whaletv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class EpgGuideSourceParserTest {
    private val parser = EpgGuideSourceParser()

    @Test
    fun parse_keepsOnlyDirectXmlSourcesForAllowedChannels() {
        val sources = parser.parse(
            """
            [
              {
                "channel": "AlJazeera.qa",
                "feed": "English",
                "site": "aljazeera.com",
                "lang": "en",
                "sources": [
                  {"format": "XML", "url": "https://example.com/aljazeera.xml"},
                  {"format": "GZIP", "url": "https://example.com/aljazeera.xml.gz"},
                  {"format": "JSON", "url": "https://example.com/aljazeera.json"}
                ]
              },
              {
                "channel": "CCTV13.cn",
                "site": "epg.example",
                "lang": "zh",
                "sources": []
              },
              {
                "channel": "Other.us",
                "sources": [
                  {"format": "XML", "url": "https://example.com/other.xml"}
                ]
              }
            ]
            """.trimIndent(),
            allowedChannelIds = setOf("AlJazeera.qa", "CCTV13.cn"),
        )

        assertEquals(2, sources.size)
        assertEquals(listOf("https://example.com/aljazeera.xml", "https://example.com/aljazeera.xml.gz"), sources.map { it.url })
        assertEquals(setOf("AlJazeera.qa"), sources.map { it.channelId }.toSet())
    }

    @Test
    fun parse_normalizesFeedSuffixAndDeduplicatesSources() {
        val sources = parser.parse(
            """
            [
              {
                "channel": "News.cn@HD",
                "sources": [
                  {"format": "XML", "url": "https://example.com/news.xml"},
                  {"format": "XML", "url": "https://example.com/news.xml"}
                ]
              }
            ]
            """.trimIndent(),
            allowedChannelIds = setOf("News.cn"),
        )

        assertEquals(1, sources.size)
        assertEquals("News.cn", sources.single().channelId)
        assertEquals("https://example.com/news.xml", sources.single().url)
    }
}
