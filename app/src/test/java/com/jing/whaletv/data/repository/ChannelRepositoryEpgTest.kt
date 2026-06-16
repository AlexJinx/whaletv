package com.jing.whaletv.data.repository

import com.jing.whaletv.data.network.FetchResult
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelRepositoryEpgTest {
    @Test
    fun testEpgSourceUrls_continuesAfterFailedSource() = runTest {
        val requestedUrls = mutableListOf<String>()

        val result = testEpgSourceUrls(
            urls = listOf("https://example.com/broken.xml", "https://example.com/working.xml"),
            allowedChannelIds = setOf("News.cn"),
            sourceLimit = 4,
            fetchText = { url ->
                requestedUrls += url
                if (url.contains("broken")) {
                    throw IOException("broken source")
                }
                FetchResult.Success(body = xmltv("News.cn"), etag = null, lastModified = null)
            },
        )

        assertTrue(result.success)
        assertEquals("节目单可用：1 个频道，1 条节目", result.message)
        assertEquals(
            listOf("https://example.com/broken.xml", "https://example.com/working.xml"),
            requestedUrls,
        )
    }

    @Test
    fun testEpgSourceUrls_reportsFailureOnlyWhenAllSourcesFail() = runTest {
        val result = testEpgSourceUrls(
            urls = listOf("https://example.com/a.xml", "https://example.com/b.xml"),
            allowedChannelIds = setOf("News.cn"),
            sourceLimit = 4,
            fetchText = { throw IOException("network down") },
        )

        assertEquals(false, result.success)
        assertEquals("测试节目单失败：network down", result.message)
    }

    private fun xmltv(channelId: String): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <programme start="20260610120000 +0800" stop="20260610123000 +0800" channel="$channelId">
                <title lang="zh">午间新闻</title>
              </programme>
            </tv>
        """.trimIndent()
    }
}
