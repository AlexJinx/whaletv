package com.jing.whaletv.ui

import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchModelsTest {
    @Test
    fun searchChannels_matchesNameIdAndGroupTitle() {
        val cctv = channel(id = "cctv13.cn", name = "CCTV-13 新闻", group = "News")
        val cgtn = channel(id = "cgtn.cn", name = "CGTN", group = "News")
        val sports = channel(id = "sports.cn", name = "体育频道", group = "Sports")
        val channels = listOf(cgtn, sports, cctv)

        assertEquals(listOf(cctv), searchChannels("13", channels))
        assertEquals(listOf(cgtn), searchChannels("CGTN", channels))
        assertEquals(listOf(cctv, cgtn), searchChannels("news", channels))
    }

    @Test
    fun searchChannels_matchesChinesePinyinInitialsAndCompactKeys() {
        val cctv = channel(id = "cctv13.cn", name = "CCTV-13 新闻", group = "News")
        val phoenix = channel(id = "phoenix.hk", name = "凤凰资讯", group = "News")
        val xinhua = channel(id = "xinhua.cn", name = "新华社电视", group = "News")
        val sports = channel(id = "sports.cn", name = "体育频道", group = "Sports")
        val channels = listOf(sports, xinhua, phoenix, cctv)

        assertTrue(searchChannels("xw", channels).contains(cctv))
        assertEquals(listOf(cctv), searchChannels("cctv13xw", channels))
        assertEquals(listOf(phoenix), searchChannels("fhzx", channels))
        assertEquals(listOf(xinhua), searchChannels("xhs", channels))
        assertEquals(listOf(xinhua), searchChannels("xhsds", channels))
        assertEquals(listOf(sports), searchChannels("ty", channels))
    }

    @Test
    fun searchChannels_matchesEnglishWordInitials() {
        val bbc = channel(id = "bbc.uk", name = "BBC News", group = "News")
        val cnn = channel(id = "cnn.us", name = "CNN International", group = "News")

        assertEquals(listOf(bbc), searchChannels("bn", listOf(cnn, bbc)))
    }

    @Test
    fun searchChannels_returnsEmptyForBlankQuery() {
        assertTrue(searchChannels("  ", listOf(channel(id = "cctv13.cn"))).isEmpty())
    }

    @Test
    fun searchKeyboardKeys_areSixBySixInAlphabetThenNumberOrder() {
        assertEquals(36, SearchKeyboardKeys.size)
        assertEquals(
            listOf(
                "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X",
                "Y", "Z", "0", "1", "2", "3",
                "4", "5", "6", "7", "8", "9",
            ),
            SearchKeyboardKeys,
        )
        assertEquals(6, SearchKeyboardKeys.chunked(6).size)
        assertTrue(SearchKeyboardKeys.chunked(6).all { it.size == 6 })
    }

    private fun channel(
        id: String,
        name: String = "测试频道",
        group: String = "General",
    ): TvChannel {
        return TvChannel(
            id = id,
            name = name,
            logoUrl = null,
            groupTitle = group,
            priority = 0,
            isFavorite = false,
            lastWatchedAt = null,
            isAvailable = true,
            streams = listOf(stream(id)),
            currentProgram = null,
            nextProgram = null,
        )
    }

    private fun stream(channelId: String): TvStream {
        return TvStream(
            channelId = channelId,
            url = "http://example.com/$channelId",
            quality = null,
            label = null,
            referrer = null,
            userAgent = null,
            healthStatus = StreamHealth.UNKNOWN,
            failureCount = 0,
            lastFailureAt = null,
            lastSuccessAt = null,
            sortOrder = 0,
        )
    }
}
