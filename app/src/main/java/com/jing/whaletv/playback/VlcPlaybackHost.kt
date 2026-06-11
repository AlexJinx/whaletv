package com.jing.whaletv.playback

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.ui.theme.WhaleTokens
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import kotlinx.coroutines.delay

data class PlaybackStatus(
    val activeStream: TvStream?,
    val message: String,
    val isBuffering: Boolean,
)

@Composable
fun VlcPlaybackHost(
    channel: TvChannel,
    modifier: Modifier = Modifier,
    onStatusChanged: (PlaybackStatus) -> Unit,
    onStreamHealthy: (TvStream) -> Unit,
    onStreamFailed: (TvStream) -> Unit,
    onStreamTimeout: (TvStream, Boolean) -> Unit,
    onNoStreamAvailable: () -> Unit,
) {
    val context = LocalContext.current
    val latestChannel by rememberUpdatedState(channel)
    val latestOnStatusChanged by rememberUpdatedState(onStatusChanged)
    val latestOnStreamHealthy by rememberUpdatedState(onStreamHealthy)
    val latestOnStreamFailed by rememberUpdatedState(onStreamFailed)
    val latestOnStreamTimeout by rememberUpdatedState(onStreamTimeout)
    val latestOnNoStreamAvailable by rememberUpdatedState(onNoStreamAvailable)
    var activeStream by remember(channel.id) { mutableStateOf(channel.preferredStream()) }
    var reportedHealthyUrl by remember(channel.id) { mutableStateOf<String?>(null) }
    var failedStreamUrls by remember(channel.id) { mutableStateOf<Set<String>>(emptySet()) }
    var bufferingStreamUrl by remember(channel.id) { mutableStateOf<String?>(null) }
    var bufferingStartedAt by remember(channel.id) { mutableLongStateOf(0L) }
    var lastBufferingEventAt by remember(channel.id) { mutableLongStateOf(0L) }
    var streamAttemptStartedAt by remember(channel.id) { mutableLongStateOf(0L) }
    var hasProgressStarted by remember(channel.id) { mutableStateOf(false) }
    var lastObservedStreamTime by remember(channel.id) { mutableLongStateOf(0L) }
    var healthyProgressSamples by remember(channel.id) { mutableStateOf(0) }
    var lastProgressObservedAt by remember(channel.id) { mutableLongStateOf(0L) }
    var stallWatchdogStartedAt by remember(channel.id) { mutableLongStateOf(0L) }

    val libVlc = remember {
        LibVLC(
            context,
            arrayListOf(
                "--network-caching=1800",
                "--live-caching=1800",
                "--http-reconnect",
                "--avcodec-hw=any",
                "--drop-late-frames",
                "--skip-frames",
            ),
        )
    }
    val mediaPlayer = remember { MediaPlayer(libVlc) }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                VLCVideoLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    mediaPlayer.attachViews(this, null, false, false)
                }
            },
        )

        if (activeStream == null) {
            Text(
                text = "暂无可用线路",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }

    DisposableEffect(mediaPlayer) {
        val listener = MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Buffering -> {
                    val url = activeStream?.url ?: return@EventListener
                    val now = System.currentTimeMillis()
                    val isFreshBuffer = bufferingStreamUrl != url
                    if (isFreshBuffer) {
                        bufferingStreamUrl = url
                        bufferingStartedAt = now
                        lastBufferingEventAt = now
                    }
                    lastBufferingEventAt = now
                    healthyProgressSamples = 0
                    latestOnStatusChanged(
                        PlaybackStatus(
                            activeStream = activeStream,
                            message = "缓冲中 ${event.buffering.coerceIn(0f, 100f).toInt()}%",
                            isBuffering = true,
                        ),
                    )
                }
                MediaPlayer.Event.Playing -> {
                    bufferingStreamUrl = null
                    bufferingStartedAt = 0L
                    val stream = activeStream
                    latestOnStatusChanged(
                        PlaybackStatus(
                            activeStream = stream,
                            message = if (reportedHealthyUrl == null) "缓冲中" else "播放中",
                            isBuffering = reportedHealthyUrl == null,
                        ),
                    )
                }
                MediaPlayer.Event.TimeChanged -> {
                    val stream = activeStream
                    val currentStreamTime = mediaPlayer.time
                    val previousObservedTime = lastObservedStreamTime
                    if (stream != null && bufferingStreamUrl == null && currentStreamTime > 0L) {
                        val now = System.currentTimeMillis()
                        if (currentStreamTime > previousObservedTime) {
                            val mediaDelta = currentStreamTime - previousObservedTime
                            lastObservedStreamTime = currentStreamTime
                            if (mediaDelta >= STREAM_HEALTH_MIN_PROGRESS_MS) {
                                lastProgressObservedAt = now
                                healthyProgressSamples = (healthyProgressSamples + 1).coerceAtMost(STREAM_HEALTH_PROGRESS_STREAK)
                            } else {
                                healthyProgressSamples = 0
                            }
                            if (
                                reportedHealthyUrl == null &&
                                streamAttemptStartedAt != 0L &&
                                now - streamAttemptStartedAt >= STREAM_HEALTH_CONFIRM_DELAY_MS &&
                                now - lastBufferingEventAt >= STREAM_HEALTH_BUFFER_STABLE_MS &&
                                healthyProgressSamples >= STREAM_HEALTH_PROGRESS_STREAK
                            ) {
                                reportedHealthyUrl = stream.url
                                latestOnStreamHealthy(stream)
                            }
                            if (mediaDelta >= STREAM_HEALTH_MIN_PROGRESS_MS) {
                                hasProgressStarted = true
                            }
                        }
                        latestOnStatusChanged(
                            PlaybackStatus(
                                activeStream = stream,
                                message = if (reportedHealthyUrl == null) "缓冲中" else "播放中",
                                isBuffering = reportedHealthyUrl == null,
                            ),
                        )
                    } else if (stream != null && currentStreamTime > previousObservedTime) {
                        lastObservedStreamTime = currentStreamTime
                    }
                }
                MediaPlayer.Event.EncounteredError, MediaPlayer.Event.EndReached -> {
                    bufferingStreamUrl = null
                    bufferingStartedAt = 0L
                    streamAttemptStartedAt = 0L
                    val failed = activeStream
                    if (failed != null) {
                        failedStreamUrls = failedStreamUrls + failed.url
                        latestOnStreamFailed(failed)
                        val next = latestChannel.nextStreamAfter(
                            failed,
                            excludedUrls = failedStreamUrls,
                        )
                        streamAttemptStartedAt = 0L
                        activeStream = next
                        latestOnStatusChanged(
                            PlaybackStatus(
                                activeStream = next,
                                message = if (next == null) "所有备用线路暂不可用" else "当前线路失败，正在切换备用线路",
                                isBuffering = next != null,
                            ),
                        )
                        if (next == null) {
                            latestOnNoStreamAvailable()
                        }
                    }
                }
                else -> {}
            }
        }
        mediaPlayer.setEventListener(listener)
        onDispose {
            mediaPlayer.setEventListener(null)
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVlc.release()
        }
    }

    LaunchedEffect(activeStream?.url, streamAttemptStartedAt) {
        val stream = activeStream ?: return@LaunchedEffect
        val startedAt = streamAttemptStartedAt
        if (startedAt == 0L) return@LaunchedEffect

        delay(STREAM_STARTUP_TIMEOUT_MS)

        if (
            streamAttemptStartedAt == startedAt &&
            !hasProgressStarted &&
            activeStream?.url == stream.url
        ) {
            failedStreamUrls = failedStreamUrls + stream.url
            val next = latestChannel.nextStreamAfter(
                stream,
                excludedUrls = failedStreamUrls,
            )
            latestOnStreamTimeout(stream, next == null)
            bufferingStreamUrl = null
            bufferingStartedAt = 0L
            streamAttemptStartedAt = 0L
            activeStream = next
            latestOnStatusChanged(
                PlaybackStatus(
                    activeStream = next,
                    message = if (next == null) "所有备用线路暂不可用" else "当前线路连接超时，正在切换备用线路",
                    isBuffering = next != null,
                ),
            )
            if (next == null) {
                latestOnNoStreamAvailable()
            }
        }
    }

    LaunchedEffect(activeStream?.url, streamAttemptStartedAt, reportedHealthyUrl) {
        val stream = activeStream ?: return@LaunchedEffect
        val startedAt = streamAttemptStartedAt
        if (startedAt == 0L) return@LaunchedEffect

        delay(STREAM_HARD_TIMEOUT_MS)

        val currentStream = activeStream
        if (
            currentStream != null &&
            currentStream.url == stream.url &&
            streamAttemptStartedAt == startedAt &&
            reportedHealthyUrl == null
        ) {
            failedStreamUrls = failedStreamUrls + currentStream.url
            val next = latestChannel.nextStreamAfter(
                currentStream,
                excludedUrls = failedStreamUrls,
            )
            latestOnStreamTimeout(currentStream, next == null)
            bufferingStreamUrl = null
            bufferingStartedAt = 0L
            streamAttemptStartedAt = 0L
            activeStream = next
            latestOnStatusChanged(
                PlaybackStatus(
                    activeStream = next,
                    message = if (next == null) "所有备用线路暂不可用" else "当前线路长时间无播放，正在切换备用线路",
                    isBuffering = next != null,
                ),
            )
            if (next == null) {
                latestOnNoStreamAvailable()
            }
        }
    }

    LaunchedEffect(activeStream?.url, stallWatchdogStartedAt, lastProgressObservedAt) {
        val stream = activeStream ?: return@LaunchedEffect
        val streamWatchdogAt = stallWatchdogStartedAt
        if (streamWatchdogAt == 0L) return@LaunchedEffect
        val observedAt = lastProgressObservedAt

        delay(STREAM_STALL_TIMEOUT_MS)

        val currentStream = activeStream
        if (
            currentStream != null &&
            currentStream.url == stream.url &&
            streamWatchdogAt == stallWatchdogStartedAt &&
            System.currentTimeMillis() - observedAt >= STREAM_STALL_TIMEOUT_MS
        ) {
            failedStreamUrls = failedStreamUrls + currentStream.url
            val next = latestChannel.nextStreamAfter(
                currentStream,
                excludedUrls = failedStreamUrls,
            )
            latestOnStreamTimeout(currentStream, next == null)
            bufferingStreamUrl = null
            bufferingStartedAt = 0L
            streamAttemptStartedAt = 0L
            stallWatchdogStartedAt = 0L
            activeStream = next
            latestOnStatusChanged(
                PlaybackStatus(
                    activeStream = next,
                    message = if (next == null) "所有备用线路暂不可用" else "当前线路无进度，正在切换备用线路",
                    isBuffering = next != null,
                ),
            )
            if (next == null) {
                latestOnNoStreamAvailable()
            }
        }
    }

    LaunchedEffect(activeStream?.url, streamAttemptStartedAt, lastProgressObservedAt) {
        val stream = activeStream ?: return@LaunchedEffect
        val startedAt = streamAttemptStartedAt
        if (startedAt == 0L) return@LaunchedEffect

        delay(STREAM_PROGRESS_TIMEOUT_MS)

        val currentStream = activeStream
        if (
            currentStream != null &&
            currentStream.url == stream.url &&
            streamAttemptStartedAt == startedAt &&
            hasProgressStarted &&
            reportedHealthyUrl == null &&
            (lastProgressObservedAt == 0L || System.currentTimeMillis() - lastProgressObservedAt >= STREAM_PROGRESS_TIMEOUT_MS)
        ) {
            failedStreamUrls = failedStreamUrls + currentStream.url
            val next = latestChannel.nextStreamAfter(
                currentStream,
                excludedUrls = failedStreamUrls,
            )
            latestOnStreamTimeout(currentStream, next == null)
            bufferingStreamUrl = null
            bufferingStartedAt = 0L
            streamAttemptStartedAt = 0L
            activeStream = next
            latestOnStatusChanged(
                PlaybackStatus(
                    activeStream = next,
                    message = if (next == null) "所有备用线路暂不可用" else "当前线路连接超时，正在切换备用线路",
                    isBuffering = next != null,
                ),
            )
            if (next == null) {
                latestOnNoStreamAvailable()
            }
        }
    }

    LaunchedEffect(activeStream, bufferingStartedAt, lastProgressObservedAt) {
        val stream = activeStream ?: run {
            if (channel.streams.isNotEmpty()) {
                latestOnNoStreamAvailable()
            }
            return@LaunchedEffect
        }
        val startedAt = bufferingStartedAt
        if (startedAt == 0L || bufferingStreamUrl != stream.url) return@LaunchedEffect

        delay(STREAM_TIMEOUT_MS)

        val currentStream = activeStream
        if (
            currentStream != null &&
            bufferingStreamUrl == currentStream.url &&
            bufferingStartedAt != 0L &&
            currentStream == stream
        ) {
            failedStreamUrls = failedStreamUrls + currentStream.url
            val next = latestChannel.nextStreamAfter(
                currentStream,
                excludedUrls = failedStreamUrls,
            )
            latestOnStreamTimeout(currentStream, next == null)
            bufferingStreamUrl = null
            bufferingStartedAt = 0L
            streamAttemptStartedAt = 0L
            activeStream = next
            latestOnStatusChanged(
                PlaybackStatus(
                    activeStream = next,
                    message = if (next == null) "所有备用线路暂不可用" else "当前线路连接超时，正在切换备用线路",
                    isBuffering = next != null,
                ),
            )
            if (next == null) {
                latestOnNoStreamAvailable()
            }
        }
    }

    LaunchedEffect(activeStream?.url, channel.id) {
        val stream = activeStream ?: return@LaunchedEffect
        reportedHealthyUrl = null
        lastObservedStreamTime = 0L
        lastProgressObservedAt = System.currentTimeMillis()
        hasProgressStarted = false
        stallWatchdogStartedAt = System.currentTimeMillis()
        bufferingStreamUrl = stream.url
        bufferingStartedAt = System.currentTimeMillis()
        lastBufferingEventAt = System.currentTimeMillis()
        healthyProgressSamples = 0
        streamAttemptStartedAt = System.currentTimeMillis()
        latestOnStatusChanged(
            PlaybackStatus(
                activeStream = stream,
                message = "正在连接直播",
                isBuffering = true,
            ),
        )
        mediaPlayer.stop()
        val media = Media(libVlc, Uri.parse(stream.url)).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=1800")
            addOption(":live-caching=1800")
            addOption(":clock-jitter=0")
            stream.userAgent?.takeIf { it.isNotBlank() }?.let { addOption(":http-user-agent=$it") }
            stream.referrer?.takeIf { it.isNotBlank() }?.let { addOption(":http-referrer=$it") }
        }
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }
}

@Composable
fun BufferingMark(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = WhaleTokens.Cyan)
    }
}

private fun TvChannel.preferredStream(): TvStream? {
    return streams
        .filter { it.healthStatus == StreamHealth.HEALTHY || it.healthStatus == StreamHealth.UNKNOWN }
        .sortedBy { it.sortOrder }
        .firstOrNull()
}

private fun TvChannel.nextStreamAfter(
    current: TvStream,
    excludedUrls: Set<String> = emptySet(),
): TvStream? {
    val orderedStreams = streams
        .filterNot { it.url == current.url }
        .sortedBy { it.sortOrder }
    val candidates = orderedStreams.filterNot { excludedUrls.contains(it.url) }
        .filter { it.healthStatus == StreamHealth.HEALTHY || it.healthStatus == StreamHealth.UNKNOWN }
    if (candidates.isEmpty()) return null

    return candidates.firstOrNull { it.healthStatus != StreamHealth.UNHEALTHY }
}

private const val STREAM_TIMEOUT_MS = 8_000L
private const val STREAM_STARTUP_TIMEOUT_MS = 12_000L
private const val STREAM_PROGRESS_TIMEOUT_MS = 8_000L
private const val STREAM_STALL_TIMEOUT_MS = 8_000L
private const val STREAM_HARD_TIMEOUT_MS = 20_000L
private const val STREAM_HEALTH_CONFIRM_DELAY_MS = 2_000L
private const val STREAM_HEALTH_BUFFER_STABLE_MS = 2_000L
private const val STREAM_HEALTH_MIN_PROGRESS_MS = 1_000L
private const val STREAM_HEALTH_PROGRESS_STREAK = 4
