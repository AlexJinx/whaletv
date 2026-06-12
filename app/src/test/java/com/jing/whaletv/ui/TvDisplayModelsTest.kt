package com.jing.whaletv.ui

import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvDisplayModelsTest {
    @Test
    fun sectionsContainFavoriteKeyAndChannelFilterMatchesFavoriteState() {
        val channels = listOf(
            channel(id = "1", name = "A", favorite = true),
            channel(id = "2", name = "B", health = StreamHealth.UNHEALTHY),
        )

        assertTrue(TvNavSections.any { it.id == "favorite" })
        assertEquals(1, channelsForSection("favorite", channels).size)
    }

    @Test
    fun homeCountryTreatsCnSuffixAndChineseChannelsAsChina() {
        val idSuffix = channel(id = "CCTV13.cn", name = "CCTV-13", group = "News")
        val chineseName = channel(id = "LocalNews", name = "北京新闻", group = "News")

        assertEquals("cn", idSuffix.homeCountryId())
        assertEquals("cn", chineseName.homeCountryId())
        assertEquals("中国", idSuffix.homeCountryLabel())
    }

    @Test
    fun homeCountryMapsKnownForeignCountries() {
        assertEquals("us", channel(id = "News.us", name = "News US", group = "News").homeCountryId())
        assertEquals("jp", channel(id = "Tokyo.jp", name = "Tokyo TV", group = "General").homeCountryId())
        assertEquals("uk", channel(id = "BBC.uk", name = "BBC News", group = "News").homeCountryId())
        assertEquals("kr", channel(id = "KBS.kr", name = "KBS Korea", group = "General").homeCountryId())
    }

    @Test
    fun homeCategoryUsesOfficialGroupTitleBuckets() {
        val channels = listOf(
            channel(id = "1.cn", group = "General"),
            channel(id = "2.cn", group = "News"),
            channel(id = "3.cn", group = "Sports"),
            channel(id = "4.cn", group = "Movies"),
            channel(id = "5.cn", group = "Undefined"),
        )

        assertEquals("general", channels[0].homeCategoryId())
        assertEquals("news", channels[1].homeCategoryId())
        assertEquals("sports", channels[2].homeCategoryId())
        assertEquals("movie", channels[3].homeCategoryId())
        assertEquals("uncategorized", channels[4].homeCategoryId())
        assertEquals(1, homeChannelsForCategory("news", channels).size)
        assertEquals(5, homeChannelsForCategory("all", channels).size)
    }

    @Test
    fun favoriteAndHistoryAreGlobalNotCountryScoped() {
        val cnFavorite = channel(id = "A.cn", name = "中国频道", favorite = true)
        val usFavorite = channel(id = "A.us", name = "US Channel", favorite = true, watchedAt = 2_000)
        val jpHistory = channel(id = "A.jp", name = "Japan Channel", watchedAt = 3_000)
        val channels = listOf(cnFavorite, usFavorite, jpHistory)

        assertEquals(listOf(cnFavorite, usFavorite), homeFavoriteChannels(channels))
        assertEquals(listOf(jpHistory, usFavorite), homeHistoryChannels(channels))
    }

    @Test
    fun homePlayableSourceCountOnlyCountsHealthyAndUnknownStreams() {
        val channel = channel(
            id = "Multi.cn",
            streams = listOf(
                stream("Multi.cn", "a", StreamHealth.HEALTHY),
                stream("Multi.cn", "b", StreamHealth.UNKNOWN),
                stream("Multi.cn", "c", StreamHealth.DEGRADED),
                stream("Multi.cn", "d", StreamHealth.UNHEALTHY),
            ),
        )

        assertEquals(2, channel.homePlayableSourceCount())
    }

    @Test
    fun homeQualityLabelPrefers4kThenHd() {
        val fourK = channel(id = "A.cn", streams = listOf(stream("A.cn", "a", quality = "4K"), stream("A.cn", "b", quality = "720p")))
        val hd = channel(id = "B.cn", streams = listOf(stream("B.cn", "a", quality = "1080p")))
        val sd = channel(id = "C.cn", streams = listOf(stream("C.cn", "a", quality = "480p")))

        assertEquals("4K", fourK.homeQualityLabel())
        assertEquals("高清", hd.homeQualityLabel())
        assertEquals(null, sd.homeQualityLabel())
    }

    private fun channel(
        id: String,
        name: String = "测试频道",
        group: String = "General",
        favorite: Boolean = false,
        watchedAt: Long? = null,
        health: StreamHealth = StreamHealth.UNKNOWN,
        streams: List<TvStream> = listOf(stream(id, "http://example.com/$id", health)),
    ): TvChannel {
        return TvChannel(
            id = id,
            name = name,
            logoUrl = null,
            groupTitle = group,
            priority = 0,
            isFavorite = favorite,
            lastWatchedAt = watchedAt,
            isAvailable = true,
            streams = streams,
            currentProgram = null,
            nextProgram = null,
        )
    }

    private fun stream(
        channelId: String,
        url: String,
        health: StreamHealth = StreamHealth.UNKNOWN,
        quality: String? = null,
    ): TvStream {
        return TvStream(
            channelId = channelId,
            url = url,
            quality = quality,
            label = null,
            referrer = null,
            userAgent = null,
            healthStatus = health,
            failureCount = 0,
            lastFailureAt = null,
            lastSuccessAt = null,
            sortOrder = 0,
        )
    }
}
