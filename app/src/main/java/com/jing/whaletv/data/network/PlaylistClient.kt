package com.jing.whaletv.data.network

import com.jing.whaletv.core.AppConstants
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaylistClient(
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun fetchText(
        url: String,
        etag: String? = null,
        lastModified: String? = null,
    ): FetchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/x-mpegURL,text/plain,application/xml,text/xml,*/*")
            .header("User-Agent", AppConstants.DEFAULT_USER_AGENT)
            .apply {
                if (!etag.isNullOrBlank()) header("If-None-Match", etag)
                if (!lastModified.isNullOrBlank()) header("If-Modified-Since", lastModified)
            }
            .build()

        client.newCall(request).execute().use { response ->
            when {
                response.code == 304 -> FetchResult.NotModified
                response.isSuccessful -> {
                    val responseBody = response.body
                    val contentType = responseBody.contentType()
                    val bytes = responseBody.bytes()
                    val decodedBytes = if (bytes.isGzipEncoded()) {
                        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                    } else {
                        bytes
                    }
                    val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                    val body = decodedBytes.toString(charset)
                    FetchResult.Success(
                        body = body,
                        etag = response.header("ETag"),
                        lastModified = response.header("Last-Modified"),
                    )
                }
                else -> throw IOException("HTTP ${response.code} while fetching $url")
            }
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(AppConstants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(AppConstants.HTTP_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

private fun ByteArray.isGzipEncoded(): Boolean {
    return size >= 2 && this[0] == 0x1F.toByte() && this[1] == 0x8B.toByte()
}

sealed interface FetchResult {
    data object NotModified : FetchResult
    data class Success(
        val body: String,
        val etag: String?,
        val lastModified: String?,
    ) : FetchResult
}
