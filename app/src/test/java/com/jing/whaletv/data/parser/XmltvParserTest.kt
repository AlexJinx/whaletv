package com.jing.whaletv.data.parser

import java.io.StringReader
import java.time.Instant
import org.junit.Test
import org.junit.Assert.assertEquals

class XmltvParserTest {
    private val parser = XmltvParser()

    @Test
    fun parse_readsProgrammesForAllowedChannelsOnly() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <programme start="20260610120000 +0800" stop="20260610123000 +0800" channel="CCTV1.cn@HD">
                <title lang="zh">新闻三十分</title>
                <desc lang="zh">午间新闻。</desc>
              </programme>
              <programme start="20260610130000 +0800" stop="20260610133000 +0800" channel="Other.cn">
                <title lang="zh">忽略</title>
              </programme>
            </tv>
        """.trimIndent()

        val programmes = parser.parse(StringReader(xml), setOf("CCTV1.cn"))

        assertEquals(1, programmes.size)
        assertEquals("CCTV1.cn", programmes.single().channelId)
        assertEquals("新闻三十分", programmes.single().title)
        assertEquals(Instant.parse("2026-06-10T04:00:00Z").toEpochMilli(), programmes.single().startAt)
        assertEquals(Instant.parse("2026-06-10T04:30:00Z").toEpochMilli(), programmes.single().endAt)
    }
}
