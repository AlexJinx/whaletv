package com.jing.whaletv.data.repository

import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.data.model.ParsedChannel
import com.jing.whaletv.data.model.ParsedStream
import com.jing.whaletv.data.network.FetchResult
import java.io.IOException
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelRepositoryScopeTest {
    @Test
    fun playlistUrlForScope_usesSelectedOfficialScopeUrl() {
        assertEquals(
            "https://iptv-org.github.io/iptv/languages/zho.m3u",
            playlistUrlForScope(PlaylistScope.LANGUAGE_ZHO),
        )
        assertEquals(
            "https://iptv-org.github.io/iptv/categories/news.m3u",
            playlistUrlForScope(PlaylistScope.CATEGORY_NEWS),
        )
    }

    @Test
    fun playlistSourcesForSync_priorityUsesSelectedScopeUrl() {
        val sources = playlistSourcesForSync(PlaylistScope.COUNTRY_CN, PlaylistSyncMode.PRIORITY)

        assertEquals(3, sources.size)
        assertEquals("Gitee raw 镜像", sources[0].label)
        assertEquals("https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/countries/cn.m3u", sources[0].url)
        assertEquals("Gitee raw 镜像全量索引兜底", sources[1].label)
        assertEquals("https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/index.m3u", sources[1].url)
        assertEquals(PlaylistScope.COUNTRY_CN, sources[1].filterScope)
        assertEquals("iptv-org 官方源", sources[2].label)
        assertEquals("https://iptv-org.github.io/iptv/countries/cn.m3u", sources[2].url)
    }

    @Test
    fun playlistSourcesForSync_zhoAddsGiteeIndexFallbackBeforeOfficialSource() {
        val sources = playlistSourcesForSync(PlaylistScope.LANGUAGE_ZHO, PlaylistSyncMode.PRIORITY)

        assertEquals(3, sources.size)
        assertEquals("https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/languages/zho.m3u", sources[0].url)
        assertEquals("https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/index.m3u", sources[1].url)
        assertEquals(PlaylistScope.LANGUAGE_ZHO, sources[1].filterScope)
        assertEquals("https://iptv-org.github.io/iptv/languages/zho.m3u", sources[2].url)
    }

    @Test
    fun filterParsedChannelsForScope_keepsChineseFallbackChannelsOnly() {
        val channels = listOf(
            parsedChannel("CCTV1.cn", "CCTV-1", "General"),
            parsedChannel("ABNChina.us", "ABN China", "Religious"),
            parsedChannel("AngelTV.in", "Angel TV Chinese", "Religious"),
            parsedChannel("BeijingSatelliteTV.cn", "BRTV 北京卫视", "General"),
            parsedChannel("NHKWorldJapan.jp", "NHK World Japan", "News"),
        )

        assertEquals(
            listOf("CCTV1.cn", "ABNChina.us", "AngelTV.in", "BeijingSatelliteTV.cn"),
            filterParsedChannelsForScope(channels, PlaylistScope.COUNTRY_CN).map { it.id },
        )
    }

    @Test
    fun playlistSourcesForSync_fullBackfillAlwaysUsesAllChannelsUrl() {
        val sources = playlistSourcesForSync(PlaylistScope.COUNTRY_CN, PlaylistSyncMode.ALL_BACKFILL)

        assertEquals(2, sources.size)
        assertEquals("https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/index.m3u", sources[0].url)
        assertEquals("https://iptv-org.github.io/iptv/index.m3u", sources[1].url)
    }

    @Test
    fun shouldBackfillAllForScope_skipsAllScopeOnly() {
        assertFalse(shouldBackfillAllForScope(PlaylistScope.ALL))
        assertTrue(shouldBackfillAllForScope(PlaylistScope.COUNTRY_CN))
        assertTrue(shouldBackfillAllForScope(PlaylistScope.CATEGORY_NEWS))
    }

    @Test
    fun missingChannelHandling_keepsHiddenStreamsForPriorityScopeAndDeletesOnlyForAllRefresh() {
        assertEquals(
            MissingChannelHandling.MARK_UNAVAILABLE,
            missingChannelHandlingForSync(PlaylistScope.COUNTRY_CN, PlaylistSyncMode.PRIORITY),
        )
        assertEquals(
            MissingChannelHandling.MARK_UNAVAILABLE_AND_DELETE_STREAMS,
            missingChannelHandlingForSync(PlaylistScope.ALL, PlaylistSyncMode.PRIORITY),
        )
        assertEquals(
            MissingChannelHandling.MARK_UNAVAILABLE_AND_DELETE_STREAMS,
            missingChannelHandlingForSync(PlaylistScope.COUNTRY_CN, PlaylistSyncMode.ALL_BACKFILL),
        )
    }

    @Test
    fun fetchFirstParsedPlaylistSource_stopsAfterFallbackSuccess() = runTest {
        val sources = listOf(
            playlistSource("gitee-scoped"),
            playlistSource("gitee-index-fallback"),
            playlistSource("official"),
        )
        val requested = mutableListOf<String>()

        val outcome = fetchFirstParsedPlaylistSource(
            sources = sources,
            fetchSource = { source ->
                requested += source.label
                when (source.label) {
                    "gitee-scoped" -> throw IOException("HTTP 451")
                    "gitee-index-fallback" -> FetchResult.Success(
                        body = "fallback",
                        etag = "etag",
                        lastModified = "last-modified",
                    )
                    else -> throw AssertionError("official source should not be requested")
                }
            },
            parseSource = { _, source -> listOf(parsedChannel("${source.label}.cn", source.label, "News")) },
        )

        assertEquals(listOf("gitee-scoped", "gitee-index-fallback"), requested)
        assertEquals(1, outcome.successCount)
        assertEquals(0, outcome.notModifiedCount)
        assertEquals(listOf("gitee-index-fallback.cn"), outcome.parsedChannels.map { it.id })
    }

    @Test
    fun fetchFirstParsedPlaylistSource_stopsAfterNotModified() = runTest {
        val sources = listOf(
            playlistSource("cached"),
            playlistSource("official"),
        )
        val requested = mutableListOf<String>()

        val outcome = fetchFirstParsedPlaylistSource(
            sources = sources,
            fetchSource = { source ->
                requested += source.label
                if (source.label == "cached") {
                    FetchResult.NotModified
                } else {
                    throw AssertionError("fallback source should not be requested after 304")
                }
            },
            parseSource = { _, source -> listOf(parsedChannel("${source.label}.cn", source.label, "News")) },
        )

        assertEquals(listOf("cached"), requested)
        assertEquals(0, outcome.successCount)
        assertEquals(1, outcome.notModifiedCount)
        assertTrue(outcome.parsedChannels.isEmpty())
    }

    @Test
    fun channelListProgramRefreshTicks_emitsImmediatelyAndThenAtInterval() = runTest {
        val clockValues = mutableListOf(100L, 200L, 300L)

        val emitted = channelListProgramRefreshTicks(
            refreshIntervalMs = 1_000L,
            nowProvider = { clockValues.removeAt(0) },
        )
            .take(3)
            .toList()

        assertEquals(listOf(100L, 200L, 300L), emitted)
    }

    private fun playlistSource(label: String): PlaylistSource {
        return PlaylistSource(
            key = label,
            url = "https://example.com/$label.m3u",
            label = label,
        )
    }

    private fun parsedChannel(id: String, name: String, groupTitle: String): ParsedChannel {
        return ParsedChannel(
            id = id,
            name = name,
            logoUrl = null,
            groupTitle = groupTitle,
            priority = 0,
            streams = listOf(
                ParsedStream(
                    channelId = id,
                    url = "http://example.com/$id.m3u8",
                    quality = null,
                    label = name,
                    referrer = null,
                    userAgent = null,
                    sortOrder = 0,
                ),
            ),
        )
    }
}
