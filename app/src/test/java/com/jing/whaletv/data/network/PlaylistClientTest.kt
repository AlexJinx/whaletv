package com.jing.whaletv.data.network

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistClientTest {
    @Test
    fun fetchText_decodesGzipXmlResponse() = runTest {
        val xml = """<?xml version="1.0"?><tv></tv>"""
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(gzip(xml).toResponseBody("application/gzip".toMediaType()))
                    .build()
            }
            .build()

        val result = PlaylistClient(okHttpClient).fetchText("https://example.com/guide.xml.gz")

        assertEquals(xml, (result as FetchResult.Success).body)
    }

    private fun gzip(value: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            gzip.write(value.toByteArray(Charsets.UTF_8))
        }
        return output.toByteArray()
    }
}
