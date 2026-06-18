package com.jing.whaletv.data.repository

import com.jing.whaletv.data.parser.EpgGuideSource
import org.junit.Assert.assertEquals
import org.junit.Test

class EpgGuideSourceDiscoveryTest {
    @Test
    fun cachedGuideSources_keepMoreCandidatesThanFetchLimit() {
        val sources = (1..10).map { index ->
            EpgGuideSource(
                channelId = "Channel$index.cn",
                url = "https://example.com/channel$index.xml",
                site = null,
                language = null,
            )
        }

        val cached = serializeCachedGuideSources(sources, limit = EPG_GUIDE_SOURCE_CACHE_LIMIT)
        val parsed = parseCachedGuideSources(cached, allowedChannelIds = sources.map { it.channelId }.toSet())

        assertEquals(10, parsed.size)
        assertEquals(EPG_GUIDE_SOURCE_FETCH_LIMIT, selectGuideSourcesForFetch(sources, EPG_GUIDE_SOURCE_FETCH_LIMIT).size)
    }

    @Test
    fun cachedGuideSources_prioritizeEarlierCandidatesWhenCacheLimitApplies() {
        val currentScope = EpgGuideSource("Wanted.cn", "https://example.com/wanted.xml", null, null)
        val oldSources = (1..EPG_GUIDE_SOURCE_CACHE_LIMIT).map { index ->
            EpgGuideSource("Old$index.cn", "https://example.com/old$index.xml", null, null)
        }

        val cached = serializeCachedGuideSources(listOf(currentScope) + oldSources, limit = EPG_GUIDE_SOURCE_CACHE_LIMIT)

        assertEquals(
            1,
            parseCachedGuideSources(cached, allowedChannelIds = setOf("Wanted.cn")).size,
        )
        assertEquals(
            0,
            parseCachedGuideSources(cached, allowedChannelIds = setOf("Old${EPG_GUIDE_SOURCE_CACHE_LIMIT}.cn")).size,
        )
    }
}
