package com.jing.whaletv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.currentProgress
import com.jing.whaletv.ui.currentTitle
import com.jing.whaletv.ui.displayGroupTitle
import com.jing.whaletv.ui.logoColor
import com.jing.whaletv.ui.logoText
import com.jing.whaletv.ui.nextTitle
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
fun ChannelCard(
    channel: TvChannel,
    onClick: () -> Unit,
    focusedScale: Float = 1.04f,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(124.dp),
        contentPadding = PaddingValues(12.dp),
        focusedScale = focusedScale,
    ) { focused ->
        val currentTitle = channel.currentTitle()
        val nextTitle = channel.nextTitle()
        val progress = channel.currentProgress()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelLogo(channel = channel, size = 36.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = WhaleTokens.PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = channel.displayGroupTitle(),
                        color = WhaleTokens.SecondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = currentTitle ?: "直播频道",
                    color = Color(0xFFB8C4D8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                nextTitle?.let {
                    Text(
                        text = "下一个: $it",
                        color = WhaleTokens.SecondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge()
                if (channel.isFavorite) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = WhaleTokens.Gold,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            progress?.let {
                ProgramProgressBar(
                    progress = it,
                    active = focused,
                )
            }
        }
    }
}

@Composable
fun CompactChannelCard(
    channel: TvChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier
            .width(128.dp)
            .height(70.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        focusedScale = 1f,
    ) { focused ->
        val currentTitle = channel.currentTitle()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(channel = channel, size = 24.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = channel.name,
                    color = WhaleTokens.PrimaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                    text = currentTitle ?: "直播频道",
                    color = if (focused) Color(0xFFB8C4D8) else WhaleTokens.SecondaryText,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
            )
            if (focused) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LiveBadge(compact = true)
                }
            }
        }
    }
}

@Composable
fun ChannelLogo(
    channel: TvChannel,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val color = channel.logoColor()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.92f), color.copy(alpha = 0.52f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = channel.logoText(),
            color = Color.White,
            fontSize = when {
                size >= 60.dp -> 16.sp
                size >= 42.dp -> 11.sp
                else -> 8.sp
            },
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
    text: String = "直播",
    compact: Boolean = false,
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = if (compact) 8.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = if (compact) 0.3.sp else 1.sp,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(WhaleTokens.Red)
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 1.dp else 2.dp),
    )
}

@Composable
fun ProgramProgressBar(
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 2.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Color.White.copy(alpha = 0.10f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(if (active) WhaleTokens.Cyan else Color(0xFF3A5070)),
        )
    }
}
