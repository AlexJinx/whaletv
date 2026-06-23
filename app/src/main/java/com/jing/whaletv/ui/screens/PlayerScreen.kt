package com.jing.whaletv.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.data.model.nextPlaybackStreamIndex
import com.jing.whaletv.data.model.playbackStreams
import com.jing.whaletv.ui.components.tvRemoteClick
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        errorMessage = "无法播放"
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
                currentStream = currentStream,
                sourceIndex = streamIndex,
                sourceTotal = streams.size,
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
            PlayerUnavailableHint(
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
        .setUserAgent(stream.userAgent?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_USER_AGENT)
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
    currentStream: TvStream?,
    sourceIndex: Int,
    sourceTotal: Int,
    canRetry: Boolean,
    canSwitchSource: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onNextSource: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platformDensity = LocalDensity.current

    Box(modifier = modifier) {
        PlayerBackButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = 24.dp),
        )
        CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale)) {
            PlayerActionRail(
                favorite = favorite,
                currentStream = currentStream,
                sourceIndex = sourceIndex,
                sourceTotal = sourceTotal,
                canRetry = canRetry,
                canSwitchSource = canSwitchSource,
                onRetry = onRetry,
                onNextSource = onNextSource,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 132.dp, end = 48.dp),
            )
        }
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
private fun PlayerBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val backgroundColor = if (focused) {
        WhaleTokens.Cyan.copy(alpha = 0.16f)
    } else {
        PlayerGlassColor.copy(alpha = 0.52f)
    }
    val borderColor = if (focused) {
        WhaleTokens.Cyan.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(if (focused) 9.dp else 0.dp, shape = PlayerButtonShape, clip = false)
            .clip(PlayerButtonShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, PlayerButtonShape)
            .onFocusChanged { focused = it.isFocused }
            .tvRemoteClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = WhaleTokens.PrimaryText,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PlayerActionRail(
    favorite: Boolean,
    currentStream: TvStream?,
    sourceIndex: Int,
    sourceTotal: Int,
    canRetry: Boolean,
    canSwitchSource: Boolean,
    onRetry: () -> Unit,
    onNextSource: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { favoriteFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .width(PlayerRailWidth)
            .clip(PlayerRailShape)
            .background(PlayerGlassColor.copy(alpha = 0.76f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), PlayerRailShape)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerRailButton(
            text = "重试",
            icon = Icons.Default.Refresh,
            contentDescription = "重试当前源",
            enabled = canRetry,
            onClick = onRetry,
        )
        PlayerRailDivider()
        PlayerRailButton(
            text = "下一个源",
            icon = Icons.Default.SkipNext,
            contentDescription = "切换下一个源",
            enabled = canSwitchSource,
            onClick = onNextSource,
        )
        PlayerRailDivider()
        PlayerRailButton(
            text = if (favorite) "已收藏" else "收藏",
            icon = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (favorite) "取消收藏" else "收藏",
            active = favorite,
            onClick = onToggleFavorite,
            modifier = Modifier.focusRequester(favoriteFocusRequester),
        )
        PlayerRailDivider()
        PlayerSourceStatusCompact(
            stream = currentStream,
            sourceIndex = sourceIndex,
            sourceTotal = sourceTotal,
        )
    }
}

@Composable
private fun PlayerRailDivider() {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.12f)),
    )
}

@Composable
private fun PlayerSourceStatusCompact(
    stream: TvStream?,
    sourceIndex: Int,
    sourceTotal: Int,
) {
    val sourceText = if (sourceTotal > 0) {
        "源 ${sourceIndex.coerceIn(0, sourceTotal - 1) + 1}/$sourceTotal"
    } else {
        "无播放源"
    }
    val qualityText = stream?.quality?.takeIf { it.isNotBlank() } ?: "未知"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerRailStatusHeight)
            .padding(horizontal = 13.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = qualityText,
            color = WhaleTokens.PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = sourceText,
            color = WhaleTokens.SecondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
    val platformDensity = LocalDensity.current
    val upcomingPrograms = schedulePrograms
        .filterNot { program -> currentProgram?.let { program.isSameScheduleProgram(it) } == true }
        .ifEmpty { listOfNotNull(nextProgram) }
        .distinctBy { "${it.channelId}|${it.startAt}|${it.title}" }
    if (currentProgram == null && upcomingPrograms.isEmpty()) return

    CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.66f),
                        ),
                    ),
                )
                .padding(start = 90.dp, end = 90.dp, top = 86.dp, bottom = 34.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .clip(PlayerPanelShape)
                    .background(PlayerGlassColor.copy(alpha = 0.76f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), PlayerPanelShape)
                    .padding(horizontal = 34.dp, vertical = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(34.dp),
            ) {
                PlayerCurrentProgramBlock(
                    program = currentProgram,
                    now = now,
                    modifier = Modifier.weight(1.95f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(128.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                PlayerUpcomingProgramBlock(
                    programs = upcomingPrograms,
                    modifier = Modifier.weight(1.05f),
                )
            }
        }
    }
}

@Composable
private fun PlayerCurrentProgramBlock(
    program: Program?,
    now: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Text(
            text = program?.title ?: "暂无当前节目单",
            color = WhaleTokens.PrimaryText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (program != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = programTimeRange(program),
                    color = WhaleTokens.TertiaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = "直播中",
                    color = WhaleTokens.Green,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            programProgress(program, now)?.let { progress ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    PlayerProgramProgressBar(
                        progress = progress,
                        modifier = Modifier.weight(1f),
                    )
                    PlayerProgramElapsedText(program = program, now = now)
                }
            }
        } else {
            Text(
                text = "等待节目单更新",
                color = WhaleTokens.TertiaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlayerProgramProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier.height(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val knobSize = 14.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.16f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(WhaleTokens.Cyan),
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - knobSize) * clampedProgress)
                .size(knobSize)
                .clip(RoundedCornerShape(999.dp))
                .background(WhaleTokens.Cyan),
        )
    }
}

@Composable
private fun PlayerProgramElapsedText(program: Program, now: Long) {
    val duration = (program.endAt - program.startAt).coerceAtLeast(0L)
    val elapsed = (now - program.startAt).coerceIn(0L, duration)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = formatProgramDuration(elapsed),
            color = WhaleTokens.Cyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = "/ ${formatProgramDuration(duration)}",
            color = WhaleTokens.TertiaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerUpcomingProgramBlock(
    programs: List<Program>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PlayerSectionLabel("接下来")
        if (programs.isEmpty()) {
            Text(
                text = "暂无后续节目",
                color = WhaleTokens.TertiaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent event.key == Key.DirectionUp || event.key == Key.DirectionDown
                        }
                        val currentIndex = listState.firstVisibleItemIndex
                        val targetIndex = when (event.key) {
                            Key.DirectionDown -> (currentIndex + 1).coerceAtMost(programs.lastIndex)
                            Key.DirectionUp -> (currentIndex - 1).coerceAtLeast(0)
                            else -> return@onPreviewKeyEvent false
                        }
                        if (targetIndex == currentIndex) {
                            return@onPreviewKeyEvent false
                        }
                        coroutineScope.launch {
                            listState.animateScrollToItem(targetIndex)
                        }
                        true
                    }
                    .focusable(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = programs,
                    key = { program -> "${program.channelId}|${program.startAt}|${program.title}" },
                ) { program ->
                    PlayerScheduleRow(program = program)
                }
            }
        }
    }
}

@Composable
private fun PlayerSectionLabel(text: String) {
    Text(
        text = text,
        color = WhaleTokens.Cyan,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

@Composable
private fun PlayerScheduleRow(program: Program) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = formatProgramTime(program.startAt),
            color = WhaleTokens.TertiaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(66.dp),
            maxLines = 1,
        )
        Text(
            text = program.title,
            color = WhaleTokens.SecondaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatProgramDuration(value: Long): String {
    val totalSeconds = (value / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
}

private fun Long.twoDigits(): String = if (this < 10L) "0$this" else toString()

@Composable
private fun PlayerRailButton(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor = when {
        !enabled -> Color.Transparent
        focused -> WhaleTokens.Cyan.copy(alpha = 0.13f)
        active -> WhaleTokens.Cyan.copy(alpha = 0.07f)
        else -> Color.Transparent
    }
    val borderColor = when {
        !enabled -> Color.Transparent
        focused -> WhaleTokens.Cyan.copy(alpha = 0.82f)
        active -> WhaleTokens.Cyan.copy(alpha = 0.28f)
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> WhaleTokens.SecondaryText.copy(alpha = 0.32f)
        active || focused -> WhaleTokens.PrimaryText
        else -> WhaleTokens.SecondaryText
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerRailButtonHeight)
            .shadow(if (focused) 8.dp else 0.dp, shape = PlayerRailItemShape, clip = false)
            .clip(PlayerRailItemShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, PlayerRailItemShape)
            .onFocusChanged { focused = it.isFocused }
            .tvRemoteClick(enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else WhaleTokens.SecondaryText.copy(alpha = 0.32f),
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = text,
            color = contentColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlayerUnavailableHint(modifier: Modifier = Modifier) {
    Text(
        text = "无法播放",
        color = WhaleTokens.PrimaryText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier,
    )
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

private val PlayerPanelShape = RoundedCornerShape(12.dp)
private val PlayerButtonShape = RoundedCornerShape(9.dp)
private val PlayerRailShape = PlayerPanelShape
private val PlayerRailItemShape = RoundedCornerShape(8.dp)
private val PlayerGlassColor = Color(0xFF111722)
private val PlayerRailWidth = 116.dp
private val PlayerRailButtonHeight = 112.dp
private val PlayerRailStatusHeight = 106.dp

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
