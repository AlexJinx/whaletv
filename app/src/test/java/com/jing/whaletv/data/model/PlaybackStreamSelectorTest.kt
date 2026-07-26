package com.jing.whaletv.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStreamSelectorTest {
    @Test
    fun playbackStreams_skipsUnsupportedProtocolsAndUnhealthyStreams() {
        val channel = channelWithStreams(
            stream(url = "ftp://example.com/video.ts", sortOrder = 0, health = StreamHealth.HEALTHY),
            stream(url = "http://example.com/dead.m3u8", sortOrder = 1, health = StreamHealth.UNHEALTHY),
            stream(url = "https://example.com/live.m3u8", sortOrder = 2, health = StreamHealth.UNKNOWN),
            stream(url = "rtsp://example.com/live", sortOrder = 3, health = StreamHealth.HEALTHY),
        )

        val urls = channel.playbackStreams().map { it.url }

        assertEquals(listOf("rtsp://example.com/live", "https://example.com/live.m3u8"), urls)
    }

    @Test
    fun playbackStreams_prefersHealthyThenSortOrder() {
        val channel = channelWithStreams(
            stream(url = "https://example.com/unknown-low.m3u8", sortOrder = 0, health = StreamHealth.UNKNOWN),
            stream(url = "https://example.com/healthy-high.m3u8", sortOrder = 4, health = StreamHealth.HEALTHY),
            stream(url = "https://example.com/healthy-low.m3u8", sortOrder = 1, health = StreamHealth.HEALTHY),
        )

        val urls = channel.playbackStreams().map { it.url }

        assertEquals(
            listOf(
                "https://example.com/healthy-low.m3u8",
                "https://example.com/healthy-high.m3u8",
                "https://example.com/unknown-low.m3u8",
            ),
            urls,
        )
    }

    @Test
    fun playbackStreams_returnsEmptyForUnavailableChannel() {
        val channel = channelWithStreams(
            stream(url = "https://example.com/live.m3u8", health = StreamHealth.HEALTHY),
            isAvailable = false,
        )

        assertEquals(emptyList<TvStream>(), channel.playbackStreams())
    }

    @Test
    fun nextPlaybackStream_skipsFailedUrls() {
        val channel = channelWithStreams(
            stream(url = "https://example.com/one.m3u8", sortOrder = 0),
            stream(url = "https://example.com/two.m3u8", sortOrder = 1),
        )

        val next = channel.nextPlaybackStream(setOf("https://example.com/one.m3u8"))

        assertEquals("https://example.com/two.m3u8", next?.url)
    }

    @Test
    fun nextPlaybackStreamIndex_wrapsForManualSourceSwitching() {
        val channel = channelWithStreams(
            stream(url = "https://example.com/one.m3u8", sortOrder = 0),
            stream(url = "https://example.com/two.m3u8", sortOrder = 1),
        )
        val streams = channel.playbackStreams()

        assertEquals(1, streams.nextPlaybackStreamIndex(0))
        assertEquals(0, streams.nextPlaybackStreamIndex(1))
        assertEquals(0, streams.nextPlaybackStreamIndex(-1))
        assertEquals(null, emptyList<TvStream>().nextPlaybackStreamIndex(0))
    }

    @Test
    fun isPlaybackSupported_acceptsHttpHttpsAndRtsp() {
        assertTrue(stream(url = "http://example.com/live.m3u8").isPlaybackSupported())
        assertTrue(stream(url = "https://example.com/live.m3u8").isPlaybackSupported())
        assertTrue(stream(url = "rtsp://example.com/live").isPlaybackSupported())
        assertFalse(stream(url = "udp://example.com/live").isPlaybackSupported())
    }

    @Test
    fun streamHealthAfterFailure_marksUnhealthyAtThreshold() {
        assertEquals(StreamHealth.UNKNOWN, streamHealthAfterFailure(1))
        assertEquals(StreamHealth.UNKNOWN, streamHealthAfterFailure(STREAM_UNHEALTHY_FAILURE_THRESHOLD - 1))
        assertEquals(StreamHealth.UNHEALTHY, streamHealthAfterFailure(STREAM_UNHEALTHY_FAILURE_THRESHOLD))
        assertEquals(StreamHealth.UNHEALTHY, streamHealthAfterFailure(STREAM_UNHEALTHY_FAILURE_THRESHOLD + 1))
    }

    private fun channelWithStreams(
        vararg streams: TvStream,
        isAvailable: Boolean = true,
    ): TvChannel {
        return TvChannel(
            id = "test.channel",
            name = "Test Channel",
            logoUrl = null,
            groupTitle = "news",
            priority = 0,
            isFavorite = false,
            lastWatchedAt = null,
            isAvailable = isAvailable,
            streams = streams.toList(),
            currentProgram = null,
            nextProgram = null,
        )
    }

    private fun stream(
        url: String,
        sortOrder: Int = 0,
        health: StreamHealth = StreamHealth.UNKNOWN,
    ): TvStream {
        return TvStream(
            channelId = "test.channel",
            url = url,
            quality = null,
            label = null,
            referrer = null,
            userAgent = null,
            healthStatus = health,
            failureCount = 0,
            lastFailureAt = null,
            lastSuccessAt = null,
            sortOrder = sortOrder,
        )
    }
}
