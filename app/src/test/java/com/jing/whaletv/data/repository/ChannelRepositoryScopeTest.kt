package com.jing.whaletv.data.repository

import com.jing.whaletv.data.model.PlaylistScope
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

        assertEquals(1, sources.size)
        assertEquals("https://iptv-org.github.io/iptv/countries/cn.m3u", sources.single().url)
    }

    @Test
    fun playlistSourcesForSync_fullBackfillAlwaysUsesAllChannelsUrl() {
        val sources = playlistSourcesForSync(PlaylistScope.COUNTRY_CN, PlaylistSyncMode.ALL_BACKFILL)

        assertEquals(1, sources.size)
        assertEquals("https://iptv-org.github.io/iptv/index.m3u", sources.single().url)
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
}
