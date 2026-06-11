package com.jing.whaletv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.playback.PlaybackStatus
import com.jing.whaletv.playback.VlcPlaybackHost
import com.jing.whaletv.ui.components.ChannelLogo
import com.jing.whaletv.ui.components.CompactChannelCard
import com.jing.whaletv.ui.components.LiveBadge
import com.jing.whaletv.ui.components.ProgramProgressBar
import com.jing.whaletv.ui.components.TvIconButton
import com.jing.whaletv.ui.components.TvTextButton
import com.jing.whaletv.ui.currentProgress
import com.jing.whaletv.ui.currentTitle
import com.jing.whaletv.ui.formatProgramTime
import com.jing.whaletv.ui.relatedPlayableChannels
import com.jing.whaletv.ui.hasEpgData
import com.jing.whaletv.ui.programProgress
import com.jing.whaletv.data.model.isPlayable
import com.jing.whaletv.ui.theme.WhaleTokens
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlayerScreen(
    channel: TvChannel,
    channels: List<TvChannel>,
    onBack: () -> Unit,
    onOpenChannel: (TvChannel) -> Unit,
    onOpenDetail: (TvChannel) -> Unit,
    onToggleFavorite: (TvChannel) -> Unit,
    onStreamHealthy: (TvStream) -> Unit,
    onStreamFailed: (TvStream) -> Unit,
    onStreamTimeout: (TvStream, Boolean) -> Unit,
    onNoStreamAvailable: (String) -> Unit,
    onWatched: (TvChannel) -> Unit,
    ) {
    var status by remember(channel.id) {
        mutableStateOf(PlaybackStatus(activeStream = null, message = "准备播放", isBuffering = true))
    }
    var hasMarkedWatched by remember(channel.id) { mutableStateOf(false) }
    val playableRelatedChannels = relatedPlayableChannels(
        current = channel,
        channels = channels,
        limit = 24,
    )
    val hasEpgData = channel.hasEpgData()
    var showEpg by remember(channel.id, hasEpgData) { mutableStateOf(hasEpgData) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val latestStatus by rememberUpdatedState(status)

    LaunchedEffect(channel.id) {
        if (!channel.isPlayable()) {
            onNoStreamAvailable(channel.id)
        }
    }

    LaunchedEffect(channel.isPlayable()) {
        if (!channel.isPlayable()) {
            onNoStreamAvailable(channel.id)
        }
    }
    val isLikelyBuffering = status.isBuffering || status.message.contains("缓冲")
    LaunchedEffect(status.activeStream?.url, isLikelyBuffering, channel.id) {
        val activeStream = status.activeStream ?: return@LaunchedEffect
        if (!isLikelyBuffering) {
            return@LaunchedEffect
        }
        val stuckStartAt = System.currentTimeMillis()
        while (true) {
            delay(1_000L)
            val current = latestStatus
            if (current.activeStream?.url != activeStream.url) return@LaunchedEffect
            if (!(current.isBuffering || current.message.contains("缓冲"))) return@LaunchedEffect
            if (System.currentTimeMillis() - stuckStartAt >= STREAM_STUCK_TIMEOUT_MS) {
                onNoStreamAvailable(channel.id)
                onStreamTimeout(activeStream, true)
                return@LaunchedEffect
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleTokens.BackgroundDeep),
    ) {
        VlcPlaybackHost(
            channel = channel,
            modifier = Modifier.fillMaxSize(),
            onStatusChanged = {
                status = it
                if (!hasMarkedWatched && !it.isBuffering && it.activeStream != null) {
                    hasMarkedWatched = true
                    onWatched(channel)
                }
            },
            onStreamHealthy = onStreamHealthy,
            onStreamFailed = onStreamFailed,
            onStreamTimeout = onStreamTimeout,
            onNoStreamAvailable = { onNoStreamAvailable(channel.id) },
        )

        PlayerTopOverlay(
            channel = channel,
            status = status,
            now = now,
            onBack = onBack,
            onToggleFavorite = { onToggleFavorite(channel) },
            onOpenDetail = { onOpenDetail(channel) },
            onToggleEpg = { showEpg = !showEpg },
        )

        if (showEpg && hasEpgData) {
            PlayerEpgPanel(
                channel = channel,
                status = status,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        PlayerBottomCarousel(
            channels = playableRelatedChannels,
            currentChannel = channel,
            onOpenChannel = onOpenChannel,
            modifier = Modifier.align(Alignment.BottomStart),
            rightInset = if (showEpg && hasEpgData) 260.dp else 0.dp,
        )
    }
}

private const val STREAM_STUCK_TIMEOUT_MS = 20_000L

@Composable
private fun PlayerTopOverlay(
    channel: TvChannel,
    status: PlaybackStatus,
    now: Long,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleEpg: () -> Unit,
) {
    val currentTitle = channel.currentTitle()
    val hasEpgData = channel.hasEpgData()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Brush.verticalGradient(listOf(Color(0xEA050810), Color.Transparent)))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvIconButton(icon = Icons.Default.ArrowBack, contentDescription = "返回", onClick = onBack)
        ChannelLogo(channel = channel, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = WhaleTokens.PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentTitle ?: "直播频道",
                color = WhaleTokens.TertiaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LiveBadge(text = if (status.isBuffering) "缓冲中" else "直播中")
        Text(status.message, color = WhaleTokens.PrimaryText, fontSize = 11.sp, maxLines = 1)
        TvIconButton(
            icon = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "收藏",
            tint = if (channel.isFavorite) WhaleTokens.Gold else WhaleTokens.SecondaryText,
            focusedTint = WhaleTokens.Gold,
            onClick = onToggleFavorite,
        )
        if (hasEpgData) {
            TvIconButton(icon = Icons.Default.List, contentDescription = "节目单", onClick = onToggleEpg)
        }
        TvTextButton(text = "详情", onClick = onOpenDetail)
        Text(
            text = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(now)),
            color = WhaleTokens.SecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.width(44.dp),
        )
    }
}

@Composable
private fun PlayerEpgPanel(
    channel: TvChannel,
    status: PlaybackStatus,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(Color(0xE00A0E16))
            .border(1.dp, Color.White.copy(alpha = 0.07f))
            .padding(top = 72.dp, bottom = 120.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text("节目单", color = WhaleTokens.SecondaryText, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(channel.name, color = WhaleTokens.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            channel.currentProgram?.let { ProgramItem(it, current = true) }
            channel.nextProgram?.let { ProgramItem(it, current = false) }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(status.message, color = WhaleTokens.Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            channel.currentProgress()?.let {
                ProgramProgressBar(it, active = true, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ProgramItem(
    program: Program,
    current: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (current) Color(0x1000C8D4) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            text = formatProgramTime(program.startAt),
            color = if (current) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
            fontSize = 10.sp,
        )
        Text(
            text = program.title,
            color = if (current) WhaleTokens.PrimaryText else WhaleTokens.TertiaryText,
            fontSize = 12.sp,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (current) {
            ProgramProgressBar(programProgress(program), active = true, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun PlayerBottomCarousel(
    channels: List<TvChannel>,
    currentChannel: TvChannel,
    onOpenChannel: (TvChannel) -> Unit,
    rightInset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .padding(end = rightInset)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF5050810))))
            .padding(horizontal = 28.dp, vertical = 18.dp),
    ) {
        Text("其他频道", color = WhaleTokens.SecondaryText, fontSize = 11.sp, letterSpacing = 0.5.sp)
        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(channels, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .border(
                            1.dp,
                            if (item.id == currentChannel.id) WhaleTokens.Cyan.copy(alpha = 0.7f) else Color.Transparent,
                            RoundedCornerShape(7.dp),
                        ),
                ) {
                    CompactChannelCard(channel = item, onClick = { onOpenChannel(item) })
                }
            }
        }
    }
}
