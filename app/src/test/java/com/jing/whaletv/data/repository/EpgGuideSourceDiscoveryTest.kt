package com.jing.whaletv.data.repository

import com.jing.whaletv.data.network.FetchResult
import com.jing.whaletv.data.parser.EpgGuideSource
import kotlinx.coroutines.test.runTest
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

    @Test
    fun discover_refetchesGuidesWithoutCacheHeadersWhenNotModifiedCacheMissesCurrentScope() = runTest {
        val state = mutableMapOf(
            "$EPG_GUIDES_STATE_KEY.etag" to "old-etag",
            "$EPG_GUIDES_STATE_KEY.last_modified" to "old-date",
            EPG_GUIDE_SOURCE_CACHE_STATE_KEY to serializeCachedGuideSources(
                listOf(EpgGuideSource("Other.cn", "https://example.com/other.xml", null, null)),
                limit = EPG_GUIDE_SOURCE_CACHE_LIMIT,
            ),
        )
        val etags = mutableListOf<String?>()
        val discovery = EpgGuideSourceDiscovery(
            fetchText = { _, etag, _ ->
                etags += etag
                if (etags.size == 1) {
                    FetchResult.NotModified
                } else {
                    FetchResult.Success(
                        body = guidesJson("Wanted.cn", "https://example.com/wanted.xml"),
                        etag = "new-etag",
                        lastModified = "new-date",
                    )
                }
            },
            readState = { key -> state[key] },
            writeState = { key, value ->
                if (value == null) {
                    state.remove(key)
                } else {
                    state[key] = value
                }
            },
        )

        val sources = discovery.discover(setOf("Wanted.cn"))

        assertEquals(listOf("old-etag", null), etags)
        assertEquals("https://example.com/wanted.xml", sources.single().url)
        assertEquals("new-etag", state["$EPG_GUIDES_STATE_KEY.etag"])
        assertEquals(
            1,
            parseCachedGuideSources(state[EPG_GUIDE_SOURCE_CACHE_STATE_KEY], setOf("Wanted.cn")).size,
        )
    }

    private fun guidesJson(channelId: String, url: String): String {
        return """
            [
              {
                "channel": "$channelId",
                "site": "example.com",
                "lang": "zh",
                "sources": [
                  {"format": "XML", "url": "$url"}
                ]
              }
            ]
        """.trimIndent()
    }
}
