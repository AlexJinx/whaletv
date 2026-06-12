package com.jing.whaletv.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.data.model.playbackStreams
import com.jing.whaletv.ui.theme.WhaleTokens

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
    val streams = remember(channel.id, channel.streams) { channel.playbackStreams() }
    var streamIndex by remember(channel.id) { mutableIntStateOf(0) }
    val currentStream = streams.getOrNull(streamIndex)
    var readyStreamUrl by remember(channel.id) { mutableStateOf<String?>(null) }
    var errorMessage by remember(channel.id) { mutableStateOf<String?>(null) }
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    BackHandler(onBack = onClose)

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player, currentStream?.url) {
        val stream = currentStream ?: return@LaunchedEffect
        readyStreamUrl = null
        errorMessage = null
        player.stop()
        player.clearMediaItems()
        player.setMediaSource(buildMediaSource(context, stream))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player, currentStream?.url, streamIndex, streams.size) {
        val stream = currentStream
        if (stream == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && readyStreamUrl != stream.url) {
                        readyStreamUrl = stream.url
                        onPlaybackReady(stream.url)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    onPlaybackFailed(stream.url)
                    val nextIndex = streamIndex + 1
                    if (nextIndex < streams.size) {
                        streamIndex = nextIndex
                    } else {
                        errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "全部播放源都无法播放"
                    }
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 3_000
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player },
        )

        PlayerTopOverlay(
            channel = channel,
            sourceText = if (streams.isEmpty()) "无可用播放源" else "源 ${streamIndex + 1}/${streams.size}",
            onClose = onClose,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (currentStream == null || errorMessage != null) {
            PlayerErrorState(
                message = errorMessage ?: "频道暂无可播放源",
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
private fun PlayerTopOverlay(
    channel: TvChannel,
    sourceText: String,
    onClose: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.76f), Color.Transparent),
                ),
            )
            .padding(horizontal = 34.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlayerOverlayButton(onClick = onClose) {
            Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = WhaleTokens.PrimaryText, modifier = Modifier.size(22.dp))
        }
        Icon(Icons.Default.Tv, contentDescription = null, tint = WhaleTokens.Cyan, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = WhaleTokens.PrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(sourceText, color = WhaleTokens.TertiaryText, fontSize = 12.sp)
        }
        PlayerOverlayButton(onClick = { onToggleFavorite(!channel.isFavorite) }) {
            Icon(
                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (channel.isFavorite) "取消收藏" else "收藏",
                tint = if (channel.isFavorite) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PlayerOverlayButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PlayerErrorState(message: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(message, color = WhaleTokens.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(WhaleTokens.Cyan.copy(alpha = 0.16f))
                .focusable()
                .clickable(onClick = onClose)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("返回首页", color = WhaleTokens.Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
