package com.jing.whaletv.data.repository

import com.jing.whaletv.data.model.PlaylistScope
import org.junit.Assert.assertEquals
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
}
