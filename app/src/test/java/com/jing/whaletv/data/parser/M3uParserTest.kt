package com.jing.whaletv.data.parser

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class M3uParserTest {
    private val parser = M3uParser()

    @Test
    fun parse_mergesFeedVariantsIntoOneChannel() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CCTV1.cn@HD" tvg-logo="https://example.com/cctv1.png" group-title="General",CCTV-1 (1080p)
            http://example.com/cctv1-hd.m3u8
            #EXTINF:-1 tvg-id="CCTV1.cn@SD" tvg-logo="https://example.com/cctv1.png" group-title="General",CCTV-1 (720p)
            http://example.com/cctv1-sd.m3u8
        """.trimIndent()

        val channels = parser.parse(m3u)

        assertEquals(1, channels.size)
        assertEquals("CCTV1.cn", channels.single().id)
        assertEquals(2, channels.single().streams.size)
        assertEquals(10, channels.single().priority)
    }

    @Test
    fun parse_readsVlcHttpOptionsForNextStream() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="News.cn@SD" group-title="News",News Channel (720p)
            #EXTVLCOPT:http-user-agent=WhaleTest
            #EXTVLCOPT:http-referrer=https://example.com/
            https://media.example.com/live.m3u8
        """.trimIndent()

        val stream = parser.parse(m3u).single().streams.single()

        assertEquals("WhaleTest", stream.userAgent)
        assertEquals("https://example.com/", stream.referrer)
        assertEquals("720p", stream.quality)
    }

    @Test
    fun parseXmltvUrl_readsPlaylistHeaderGuideUrl() {
        val m3u = """
            #EXTM3U x-tvg-url="https://example.com/guide.xml.gz"
            #EXTINF:-1 tvg-id="News.cn@SD" group-title="News",News Channel
            https://media.example.com/live.m3u8
        """.trimIndent()

        assertEquals("https://example.com/guide.xml.gz", parser.parseXmltvUrl(m3u))
    }

    @Test
    fun parse_prioritizesSatelliteBeforeOtherChannels() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="BreadTV.cn@SD" group-title="Undefined",Bread TV面包台 (720p)
            https://example.com/bread.m3u8
            #EXTINF:-1 tvg-id="BeijingSatelliteTV.cn@HD" group-title="General",BRTV 北京卫视 (1080p)
            https://example.com/brtv.m3u8
        """.trimIndent()

        val channels = parser.parse(m3u)

        assertEquals("BeijingSatelliteTV.cn", channels.first().id)
        assertTrue(channels.first().priority < channels.last().priority)
    }

    @Test
    fun parse_handlesMissingTvgId() {
        val channels = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="News",临时频道 (576p)
            https://example.com/tmp.m3u8
            """.trimIndent(),
        )

        assertNotNull(channels.single().id)
        assertEquals("临时频道", channels.single().name)
    }

    @Test
    fun parse_doesNotCarryChannelMetadataAcrossCalls() {
        val first = """
            #EXTM3U
            #EXTINF:-1 tvg-id="First.cn@HD" tvg-logo="https://example.com/first.png" group-title="News",First News
            https://example.com/first.m3u8
        """.trimIndent()
        val second = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Second.cn@HD" group-title="Sports",Second Sports
            https://example.com/second.m3u8
        """.trimIndent()

        parser.parse(first)
        val channel = parser.parse(second).single()

        assertEquals("Second.cn", channel.id)
        assertEquals("Sports", channel.groupTitle)
        assertEquals(null, channel.logoUrl)
        assertEquals("Second Sports", channel.name)
    }
}
