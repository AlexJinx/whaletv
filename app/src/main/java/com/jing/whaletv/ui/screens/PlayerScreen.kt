package com.jing.whaletv.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.data.model.nextPlaybackStreamIndex
import com.jing.whaletv.data.model.playbackStreams
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: TvChannel,
    onClose: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onPlaybackReady: (String) -> Unit,
    onPlaybackFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val screenFocusRequester = remember { FocusRequester() }
    val streams = remember(channel.id, channel.streams) { channel.playbackStreams() }
    var streamIndex by remember(channel.id) { mutableIntStateOf(0) }
    val currentStream = streams.getOrNull(streamIndex)
    var retryNonce by remember(channel.id) { mutableIntStateOf(0) }
    var readyStreamUrl by remember(channel.id) { mutableStateOf<String?>(null) }
    var errorMessage by remember(channel.id) { mutableStateOf<String?>(null) }
    var playbackState by remember(channel.id) { mutableIntStateOf(Player.STATE_IDLE) }
    var overlayVisible by remember(channel.id) { mutableStateOf(true) }
    var overlayRevealTick by remember(channel.id) { mutableIntStateOf(0) }
    var favorite by remember(channel.id, channel.isFavorite) { mutableStateOf(channel.isFavorite) }
    var now by remember(channel.id) { mutableLongStateOf(System.currentTimeMillis()) }
    val reportedFailedUrls = remember(channel.id) { mutableSetOf<String>() }
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    fun revealOverlay() {
        overlayVisible = true
        overlayRevealTick += 1
    }

    fun retryCurrentSource() {
        if (currentStream == null) return
        errorMessage = null
        playbackState = Player.STATE_IDLE
        retryNonce += 1
        revealOverlay()
    }

    fun switchToNextSource() {
        val nextIndex = streams.nextPlaybackStreamIndex(streamIndex) ?: return
        errorMessage = null
        playbackState = Player.STATE_IDLE
        if (nextIndex == streamIndex) {
            retryNonce += 1
        } else {
            streamIndex = nextIndex
        }
        revealOverlay()
    }

    BackHandler(onBack = onClose)

    LaunchedEffect(channel.id) {
        runCatching { screenFocusRequester.requestFocus() }
    }

    LaunchedEffect(channel.currentProgram?.startAt, channel.currentProgram?.endAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(PROGRAM_PROGRESS_TICK_MS)
        }
    }

    LaunchedEffect(overlayVisible, overlayRevealTick, playbackState, errorMessage, currentStream?.url) {
        if (
            overlayVisible &&
            playbackState == Player.STATE_READY &&
            errorMessage == null &&
            currentStream != null
        ) {
            delay(PLAYER_OVERLAY_AUTO_HIDE_MS)
            overlayVisible = false
            runCatching { screenFocusRequester.requestFocus() }
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player, currentStream?.url, retryNonce) {
        val stream = currentStream
        if (stream == null) {
            playbackState = Player.STATE_IDLE
            player.stop()
            player.clearMediaItems()
            return@LaunchedEffect
        }

        readyStreamUrl = null
        errorMessage = null
        playbackState = Player.STATE_BUFFERING
        player.stop()
        player.clearMediaItems()
        player.setMediaSource(buildMediaSource(context, stream))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player, currentStream?.url, streamIndex, streams.size, retryNonce) {
        val stream = currentStream
        if (stream == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    playbackState = state
                    if (state == Player.STATE_READY && readyStreamUrl != stream.url) {
                        readyStreamUrl = stream.url
                        onPlaybackReady(stream.url)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playbackState = Player.STATE_IDLE
                    if (reportedFailedUrls.add(stream.url)) {
                        onPlaybackFailed(stream.url)
                    }
                    val nextIndex = streamIndex + 1
                    if (nextIndex < streams.size) {
                        streamIndex = nextIndex
                    } else {
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "全部播放源都无法播放"
                    }
                    revealOverlay()
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key in PlayerOverlayRevealKeys) {
                    revealOverlay()
                }
                false
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            },
            update = { it.player = player },
        )

        if (overlayVisible || currentStream == null || errorMessage != null) {
            PlayerOverlay(
                channel = channel,
                favorite = favorite,
                now = now,
                sourceText = sourceLabel(currentStream, streamIndex, streams.size),
                statusText = playbackStatusText(playbackState, currentStream, errorMessage),
                statusColor = playbackStatusColor(playbackState, currentStream, errorMessage),
                canRetry = currentStream != null,
                canSwitchSource = streams.size > 1,
                onClose = onClose,
                onRetry = ::retryCurrentSource,
                onNextSource = ::switchToNextSource,
                onToggleFavorite = {
                    val nextFavorite = !favorite
                    favorite = nextFavorite
                    onToggleFavorite(nextFavorite)
                    revealOverlay()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (currentStream == null || errorMessage != null) {
            PlayerErrorState(
                message = errorMessage ?: "频道暂无可播放源",
                canRetry = currentStream != null,
                canSwitchSource = streams.size > 1,
                onRetry = ::retryCurrentSource,
                onNextSource = ::switchToNextSource,
                onClose = onClose,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun buildMediaSource(context: Context, stream: TvStream): MediaSource {
    val requestHeaders = mutableMapOf<String, String>()
    stream.referrer?.takeIf { it.isNotBlank() }?.let { requestHeaders["Referer"] = it }
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(stream.userAgent?.takeIf { it.isNotBlank() } ?: "WhaleTV/1.0 AndroidTV")
        .setDefaultRequestProperties(requestHeaders)
    return DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(stream.url))
}

@Composable
private fun PlayerOverlay(
    channel: TvChannel,
    favorite: Boolean,
    now: Long,
    sourceText: String,
    statusText: String,
    statusColor: Color,
    canRetry: Boolean,
    canSwitchSource: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onNextSource: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PlayerTopOverlay(
            channel = channel,
            favorite = favorite,
            sourceText = sourceText,
            statusText = statusText,
            statusColor = statusColor,
            canRetry = canRetry,
            canSwitchSource = canSwitchSource,
            onClose = onClose,
            onRetry = onRetry,
            onNextSource = onNextSource,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        PlayerProgramOverlay(
            currentProgram = channel.currentProgram,
            nextProgram = channel.nextProgram,
            schedulePrograms = channel.schedulePrograms,
            now = now,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PlayerTopOverlay(
    channel: TvChannel,
    favorite: Boolean,
    sourceText: String,
    statusText: String,
    statusColor: Color,
    canRetry: Boolean,
    canSwitchSource: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onNextSource: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.86f), Color.Black.copy(alpha = 0.34f), Color.Transparent),
                ),
            )
            .padding(horizontal = 34.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerOverlayButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = WhaleTokens.PrimaryText, modifier = Modifier.size(22.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = channel.name,
                color = WhaleTokens.PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PlayerInfoChip(text = statusText, color = statusColor)
            PlayerInfoChip(text = sourceText, color = WhaleTokens.TertiaryText)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerOverlayButton(onClick = onRetry, enabled = canRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "重试当前源", tint = buttonIconColor(canRetry), modifier = Modifier.size(21.dp))
            }
            PlayerOverlayButton(onClick = onNextSource, enabled = canSwitchSource) {
                Icon(Icons.Default.SkipNext, contentDescription = "切换下一个源", tint = buttonIconColor(canSwitchSource), modifier = Modifier.size(22.dp))
            }
            PlayerOverlayButton(onClick = onToggleFavorite, active = favorite) {
                Icon(
                    imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "取消收藏" else "收藏",
                    tint = if (favorite) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerInfoChip(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF151921).copy(alpha = 0.48f))
            .border(1.dp, Color.White.copy(alpha = 0.045f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun PlayerProgramOverlay(
    currentProgram: Program?,
    nextProgram: Program?,
    schedulePrograms: List<Program>,
    now: Long,
    modifier: Modifier = Modifier,
) {
    val upcomingPrograms = schedulePrograms
        .filterNot { program -> currentProgram?.let { program.isSameScheduleProgram(it) } == true }
        .ifEmpty { listOfNotNull(nextProgram) }
        .distinctBy { "${it.channelId}|${it.startAt}|${it.title}" }
        .take(PLAYER_SCHEDULE_UPCOMING_LIMIT)
    if (currentProgram == null && upcomingPrograms.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f), Color.Black.copy(alpha = 0.86f)),
                ),
            )
            .padding(start = 40.dp, end = 40.dp, top = 74.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        currentProgram?.let { program ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("正在播出", color = WhaleTokens.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = program.title,
                    color = WhaleTokens.PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(programTimeRange(program), color = WhaleTokens.TertiaryText, fontSize = 12.sp)
            }
            programProgress(program, now)?.let { progress ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(WhaleTokens.Cyan),
                    )
                }
            }
        }
        if (upcomingPrograms.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                upcomingPrograms.forEachIndexed { index, program ->
                    PlayerScheduleRow(
                        label = if (index == 0) "接下来" else "稍后",
                        program = program,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerScheduleRow(label: String, program: Program) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            color = if (label == "接下来") WhaleTokens.Cyan else WhaleTokens.TertiaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = programTimeRange(program),
            color = WhaleTokens.TertiaryText,
            fontSize = 12.sp,
            modifier = Modifier.width(82.dp),
        )
        Text(
            text = program.title,
            color = WhaleTokens.SecondaryText,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlayerOverlayButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = when {
        !enabled -> Color(0xFF151921).copy(alpha = 0.30f)
        active || focused -> WhaleTokens.Cyan.copy(alpha = 0.14f)
        else -> Color(0xFF151921).copy(alpha = 0.72f)
    }
    val borderColor = when {
        !enabled -> Color.White.copy(alpha = 0.045f)
        focused -> WhaleTokens.Cyan.copy(alpha = 0.72f)
        active -> WhaleTokens.Cyan.copy(alpha = 0.32f)
        else -> Color.White.copy(alpha = 0.10f)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(if (focused) 9.dp else 0.dp, shape = shape, clip = false)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PlayerErrorState(
    message: String,
    canRetry: Boolean,
    canSwitchSource: Boolean,
    onRetry: () -> Unit,
    onNextSource: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(message, color = WhaleTokens.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerTextButton(text = "重试", enabled = canRetry, onClick = onRetry)
            PlayerTextButton(text = "下一个源", enabled = canSwitchSource, onClick = onNextSource)
            PlayerTextButton(text = "返回首页", enabled = true, onClick = onClose)
        }
    }
}

@Composable
private fun PlayerTextButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) WhaleTokens.Cyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (enabled) WhaleTokens.Cyan.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(8.dp),
            )
            .focusable(enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun playbackStatusText(
    playbackState: Int,
    currentStream: TvStream?,
    errorMessage: String?,
): String {
    return when {
        currentStream == null -> "无可用播放源"
        errorMessage != null -> "播放失败"
        playbackState == Player.STATE_BUFFERING -> "正在缓冲"
        playbackState == Player.STATE_READY -> "播放中"
        playbackState == Player.STATE_ENDED -> "已结束"
        else -> "准备播放"
    }
}

private fun playbackStatusColor(
    playbackState: Int,
    currentStream: TvStream?,
    errorMessage: String?,
): Color {
    return when {
        currentStream == null || errorMessage != null -> WhaleTokens.Red
        playbackState == Player.STATE_READY -> WhaleTokens.Green
        playbackState == Player.STATE_BUFFERING -> WhaleTokens.Cyan
        else -> WhaleTokens.TertiaryText
    }
}

private fun sourceLabel(stream: TvStream?, index: Int, total: Int): String {
    if (total == 0) return "无可用播放源"
    val quality = stream?.quality?.takeIf { it.isNotBlank() }
    return buildString {
        append("源 ${index + 1}/$total")
        if (quality != null) append(" · $quality")
    }
}

private fun programProgress(program: Program, now: Long): Float? {
    val duration = program.endAt - program.startAt
    if (duration <= 0L) return null
    return ((now - program.startAt).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun programTimeRange(program: Program): String {
    return "${formatProgramTime(program.startAt)}-${formatProgramTime(program.endAt)}"
}

private fun Program.isSameScheduleProgram(other: Program): Boolean {
    return channelId == other.channelId && startAt == other.startAt && title == other.title
}

private fun formatProgramTime(value: Long): String {
    return ProgramTimeFormatter.format(Instant.ofEpochMilli(value))
}

private fun buttonIconColor(enabled: Boolean): Color {
    return if (enabled) WhaleTokens.PrimaryText else WhaleTokens.SecondaryText.copy(alpha = 0.38f)
}

private val PlayerOverlayRevealKeys = setOf(
    Key.DirectionUp,
    Key.DirectionDown,
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter,
)

private val ProgramTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private const val PLAYER_OVERLAY_AUTO_HIDE_MS = 5_000L
private const val PROGRAM_PROGRESS_TICK_MS = 30_000L
private const val PLAYER_SCHEDULE_UPCOMING_LIMIT = 5
