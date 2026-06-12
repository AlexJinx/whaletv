package com.jing.whaletv.data.model

import java.net.URI
import java.util.Locale

private val PlaybackSupportedSchemes = setOf("http", "https", "rtsp")

fun TvChannel.playbackStreams(): List<TvStream> {
    return streams
        .asSequence()
        .filter { it.isPlaybackSupported() }
        .filter { it.healthStatus == StreamHealth.HEALTHY || it.healthStatus == StreamHealth.UNKNOWN }
        .sortedWith(
            compareBy<TvStream> { if (it.healthStatus == StreamHealth.HEALTHY) 0 else 1 }
                .thenBy { it.sortOrder }
                .thenBy { it.url },
        )
        .toList()
}

fun TvChannel.nextPlaybackStream(failedUrls: Set<String>): TvStream? {
    return playbackStreams().firstOrNull { it.url !in failedUrls }
}

fun TvStream.isPlaybackSupported(): Boolean {
    return runCatching {
        URI(url).scheme?.lowercase(Locale.ROOT) in PlaybackSupportedSchemes
    }.getOrDefault(false)
}
