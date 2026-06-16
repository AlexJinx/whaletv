package com.jing.whaletv.data.model

import com.jing.whaletv.core.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistScopeTest {
    @Test
    fun playlistScope_mapsOfficialPresetsToIptvOrgUrls() {
        val urls = PlaylistScope.entries.associate { it.id to it.playlistUrl }

        assertEquals(AppConstants.PRIMARY_PLAYLIST_URL, urls["all"])
        assertEquals("https://iptv-org.github.io/iptv/countries/cn.m3u", urls["country_cn"])
        assertEquals("https://iptv-org.github.io/iptv/countries/us.m3u", urls["country_us"])
        assertEquals("https://iptv-org.github.io/iptv/countries/jp.m3u", urls["country_jp"])
        assertEquals("https://iptv-org.github.io/iptv/countries/uk.m3u", urls["country_uk"])
        assertEquals("https://iptv-org.github.io/iptv/countries/kr.m3u", urls["country_kr"])
        assertEquals("https://iptv-org.github.io/iptv/languages/zho.m3u", urls["language_zho"])
        assertEquals("https://iptv-org.github.io/iptv/languages/eng.m3u", urls["language_eng"])
        assertEquals("https://iptv-org.github.io/iptv/languages/jpn.m3u", urls["language_jpn"])
        assertEquals("https://iptv-org.github.io/iptv/languages/kor.m3u", urls["language_kor"])
        assertEquals("https://iptv-org.github.io/iptv/categories/news.m3u", urls["category_news"])
    }

    @Test
    fun playlistScope_fromIdFallsBackToAll() {
        assertEquals(PlaylistScope.COUNTRY_CN, PlaylistScope.fromId("country_cn"))
        assertEquals(PlaylistScope.ALL, PlaylistScope.fromId("missing"))
        assertEquals(PlaylistScope.ALL, PlaylistScope.fromId(null))
    }
}
