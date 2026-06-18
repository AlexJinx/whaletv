package com.jing.whaletv.ui

import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvDisplayModelsTest {
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
    fun homeCategoryAddsCctvVirtualBucket() {
        val cctv1 = channel(id = "CCTV1.cn", name = "CCTV-1", group = "General")
        val cctv13 = channel(id = "CCTV13.cn", name = "CCTV-13", group = "News")
        val news = channel(id = "News.cn", name = "普通新闻", group = "News")
        val channels = listOf(cctv13, news, cctv1)

        assertEquals("cctv", cctv1.homeCategoryId())
        assertEquals("cctv", cctv13.homeCategoryId())
        assertEquals(listOf(cctv13, cctv1), homeChannelsForCategory("cctv", channels))
        assertEquals(1, cctv1.cctvSortKey())
        assertEquals(13, cctv13.cctvSortKey())
        assertEquals(4, channel(id = "CCTV4K.cn", name = "CCTV-4K").cctvSortKey())
        assertEquals(Int.MAX_VALUE, channel(id = "CCTVPlus1.cn", name = "CCTV+ 1").cctvSortKey())
    }

    @Test
    fun homeCategoryAddsSatelliteVirtualBucketWithoutCctv() {
        val satellite = channel(id = "BeijingSatelliteTV.cn", name = "BRTV 北京卫视", group = "General")
        val cctv = channel(id = "CCTV4Asia.cn", name = "CCTV-4 Asia", group = "General")
        val general = channel(id = "General.cn", name = "综合频道", group = "General")
        val channels = listOf(satellite, cctv, general)

        assertEquals("satellite", satellite.homeCategoryId())
        assertEquals("cctv", cctv.homeCategoryId())
        assertEquals(listOf(satellite), homeChannelsForCategory("satellite", channels))
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

    @Test
    fun channelCardItemOnlyShowsEpgWhenProgramsExist() {
        assertFalse(channel(id = "cctv13.cn").toChannelCardItem().hasEpg)

        assertTrue(
            channel(
                id = "current.cn",
                currentProgram = program("正在播出"),
            ).toChannelCardItem().hasEpg,
        )
        assertTrue(
            channel(
                id = "next.cn",
                nextProgram = program("接下来"),
            ).toChannelCardItem().hasEpg,
        )
        assertTrue(
            channel(
                id = "schedule.cn",
                schedulePrograms = listOf(program("稍后")),
            ).toChannelCardItem().hasEpg,
        )
    }

    private fun channel(
        id: String,
        name: String = "测试频道",
        group: String = "General",
        favorite: Boolean = false,
        watchedAt: Long? = null,
        health: StreamHealth = StreamHealth.UNKNOWN,
        streams: List<TvStream> = listOf(stream(id, "http://example.com/$id", health)),
        currentProgram: Program? = null,
        nextProgram: Program? = null,
        schedulePrograms: List<Program> = emptyList(),
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
            currentProgram = currentProgram,
            nextProgram = nextProgram,
            schedulePrograms = schedulePrograms,
        )
    }

    private fun program(title: String): Program = Program(
        channelId = "test.cn",
        title = title,
        startAt = 1_000L,
        endAt = 2_000L,
        description = null,
    )

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
