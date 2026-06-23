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
    fun homeCountryDoesNotMatchShortCodesInsideWords() {
        val channel = channel(id = "MusicPlus", name = "Music Plus", group = "Music")

        assertEquals("other", channel.homeCountryId())
    }

    @Test
    fun homeCountryEntriesCountSyncedCountries() {
        val channels = listOf(
            channel(id = "CCTV1.cn", name = "CCTV-1"),
            channel(id = "CCTV2.cn", name = "CCTV-2"),
            channel(id = "News.us", name = "US News"),
            channel(id = "Arte.fr", name = "Arte"),
        )

        val entries = homeCountryEntries(channels).associateBy { it.id }

        assertEquals(2, entries.getValue("cn").channelCount)
        assertEquals(1, entries.getValue("us").channelCount)
        assertEquals(1, entries.getValue("fr").channelCount)
        assertEquals("法国", entries.getValue("fr").label)
    }

    @Test
    fun homeCountryTabsKeepChinaLockedAndNormalizeLimit() {
        val ids = listOf("us", "cn", "jp", "us") + (1..30).map { "x$it" }
        val normalized = normalizeHomeCountryTabIds(ids)

        assertEquals("cn", normalized.first())
        assertEquals(MAX_HOME_COUNTRY_TABS, normalized.size)
        assertEquals(1, normalized.count { it == "us" })
    }

    @Test
    fun defaultHomeCountryTabsIncludeLockedChina() {
        val tabs = homeCountryTabsForIds(HomeCountryTabs.map { it.id }, emptyList())

        assertEquals("cn", tabs.first().id)
        assertTrue(tabs.first().locked)
        assertEquals("中国", tabs.first().label)
    }

    @Test
    fun addableHomeCountryEntriesSearchesOnlyHiddenCountries() {
        val entries = listOf(
            CountryEntry("cn", "中国", 2, locked = true),
            CountryEntry("us", "美国", 1),
            CountryEntry("fr", "法国", 1),
            CountryEntry("de", "德国", 1),
        )

        val result = addableHomeCountryEntries(entries, visibleIds = listOf("cn", "us"), query = "法")

        assertEquals(listOf("fr"), result.map { it.id })
    }

    @Test
    fun addHomeCountryTabStopsAtMax() {
        val full = normalizeHomeCountryTabIds(listOf("cn") + (1 until MAX_HOME_COUNTRY_TABS).map { "x$it" })

        assertEquals(MAX_HOME_COUNTRY_TABS, addHomeCountryTab(full, "fr").size)
        assertFalse(addHomeCountryTab(full, "fr").contains("fr"))
    }

    @Test
    fun countryEditorRulesKeepChinaLocked() {
        val ids = listOf("cn", "us", "jp", "uk")

        assertEquals(ids, removeHomeCountryTab(ids, "cn"))
        assertEquals(ids, moveHomeCountryTab(ids, "cn", 1))
        assertEquals(ids, moveHomeCountryTab(ids, "us", -1))
        assertEquals(listOf("cn", "jp", "us", "uk"), moveHomeCountryTab(ids, "us", 1))
        assertEquals(listOf("cn", "jp", "uk"), removeHomeCountryTab(ids, "us"))
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
    fun homeCategorySpecsOnlyShowCctvAndSatelliteForChina() {
        val chinaCategoryIds = homeCategorySpecsForCountry("cn").map { it.id }

        assertTrue(chinaCategoryIds.contains("cctv"))
        assertTrue(chinaCategoryIds.contains("satellite"))

        listOf("us", "jp", "uk", "kr").forEach { countryId ->
            val categoryIds = homeCategorySpecsForCountry(countryId).map { it.id }

            assertFalse(categoryIds.contains("cctv"))
            assertFalse(categoryIds.contains("satellite"))
            assertTrue(categoryIds.contains("all"))
            assertTrue(categoryIds.contains("general"))
            assertTrue(categoryIds.contains("news"))
            assertTrue(categoryIds.contains("uncategorized"))
        }
    }

    @Test
    fun homeCategoryNormalizationFallsBackWhenCountryHidesCategory() {
        assertEquals("cctv", normalizeHomeCategoryIdForCountry("cn", "cctv"))
        assertEquals("satellite", normalizeHomeCategoryIdForCountry("cn", "satellite"))
        assertEquals("all", normalizeHomeCategoryIdForCountry("us", "cctv"))
        assertEquals("all", normalizeHomeCategoryIdForCountry("jp", "satellite"))
        assertEquals("news", normalizeHomeCategoryIdForCountry("kr", "news"))
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
    fun homePlaybackSortRankPrefersHealthyThenUnknownThenUnavailable() {
        val healthy = channel(id = "healthy.cn", streams = listOf(stream("healthy.cn", "https://example.com/live.m3u8", StreamHealth.HEALTHY)))
        val unknown = channel(id = "unknown.cn", streams = listOf(stream("unknown.cn", "https://example.com/live.m3u8", StreamHealth.UNKNOWN)))
        val unhealthy = channel(id = "unhealthy.cn", streams = listOf(stream("unhealthy.cn", "https://example.com/live.m3u8", StreamHealth.UNHEALTHY)))
        val unsupported = channel(id = "unsupported.cn", streams = listOf(stream("unsupported.cn", "ftp://example.com/live.ts", StreamHealth.HEALTHY)))
        val empty = channel(id = "empty.cn", streams = emptyList())

        assertEquals(0, healthy.homePlaybackSortRank())
        assertEquals(1, unknown.homePlaybackSortRank())
        assertEquals(2, unhealthy.homePlaybackSortRank())
        assertEquals(2, unsupported.homePlaybackSortRank())
        assertEquals(2, empty.homePlaybackSortRank())
    }

    @Test
    fun homeBrowseComparatorSortsByPlaybackHealthBeforeExistingOrder() {
        val unhealthy = channel(id = "a.cn", name = "A 频道", priority = 0, health = StreamHealth.UNHEALTHY)
        val unknown = channel(id = "b.cn", name = "B 频道", priority = 0, health = StreamHealth.UNKNOWN)
        val healthy = channel(id = "c.cn", name = "C 频道", priority = 99, health = StreamHealth.HEALTHY)

        val sorted = listOf(unhealthy, unknown, healthy).sortedWith(homeBrowseChannelComparator())

        assertEquals(listOf(healthy, unknown, unhealthy), sorted)
    }

    @Test
    fun homeBrowseComparatorKeepsExistingPriorityInsideSamePlaybackGroup() {
        val laterPriority = channel(id = "later.cn", name = "A 频道", priority = 2, health = StreamHealth.HEALTHY)
        val earlierPriority = channel(id = "earlier.cn", name = "B 频道", priority = 1, health = StreamHealth.HEALTHY)

        val sorted = listOf(laterPriority, earlierPriority).sortedWith(homeBrowseChannelComparator())

        assertEquals(listOf(earlierPriority, laterPriority), sorted)
    }

    @Test
    fun homeCctvComparatorKeepsNumberOrderAndUsesPlaybackInsideSameNumber() {
        val cctv2Healthy = channel(id = "CCTV2.cn", name = "CCTV-2", health = StreamHealth.HEALTHY)
        val cctv1Unknown = channel(id = "CCTV1Unknown.cn", name = "CCTV-1 Alpha", health = StreamHealth.UNKNOWN)
        val cctv1Healthy = channel(id = "CCTV1Healthy.cn", name = "CCTV-1 Beta", health = StreamHealth.HEALTHY)

        val sorted = listOf(cctv2Healthy, cctv1Unknown, cctv1Healthy).sortedWith(homeCctvChannelComparator())

        assertEquals(listOf(cctv1Healthy, cctv1Unknown, cctv2Healthy), sorted)
    }

    @Test
    fun homeSatelliteComparatorKeepsNameOrderAndUsesPlaybackInsideSameName() {
        val betaHealthy = channel(id = "BetaSatelliteTV.cn", name = "Beta 卫视", health = StreamHealth.HEALTHY)
        val alphaUnknown = channel(id = "AlphaUnknown.cn", name = "Alpha 卫视", health = StreamHealth.UNKNOWN)
        val alphaHealthy = channel(id = "AlphaHealthy.cn", name = "Alpha 卫视", health = StreamHealth.HEALTHY)

        val sorted = listOf(betaHealthy, alphaUnknown, alphaHealthy).sortedWith(homeSatelliteChannelComparator())

        assertEquals(listOf(alphaHealthy, alphaUnknown, betaHealthy), sorted)
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
        priority: Int = 0,
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
            priority = priority,
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
