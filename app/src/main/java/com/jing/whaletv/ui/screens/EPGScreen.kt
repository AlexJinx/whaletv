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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.components.ChannelLogo
import com.jing.whaletv.ui.components.LiveBadge
import com.jing.whaletv.ui.components.ProgramProgressBar
import com.jing.whaletv.ui.components.TvIconButton
import com.jing.whaletv.ui.components.TvTextButton
import com.jing.whaletv.ui.displayGroupTitle
import com.jing.whaletv.ui.formatProgramTime
import com.jing.whaletv.ui.logoColor
import com.jing.whaletv.ui.programProgress
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EPGScreen(
    channel: TvChannel,
    onBack: () -> Unit,
    onPlay: (TvChannel) -> Unit,
    onToggleFavorite: (TvChannel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleTokens.Background),
    ) {
        EpgHeader(channel = channel, onBack = onBack)
        Row(modifier = Modifier.fillMaxSize()) {
            ChannelDetailPanel(
                channel = channel,
                onPlay = { onPlay(channel) },
                onToggleFavorite = { onToggleFavorite(channel) },
            )
            ProgramTimeline(
                channel = channel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EpgHeader(channel: TvChannel, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvIconButton(icon = Icons.Default.ArrowBack, contentDescription = "返回", onClick = onBack)
        Box(Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.08f)))
        Text("频道详情", color = WhaleTokens.SecondaryText, fontSize = 13.sp)
        Text(channel.name, color = WhaleTokens.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChannelDetailPanel(
    channel: TvChannel,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(360.dp)
            .fillMaxHeight()
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(channel.logoColor().copy(alpha = 0.22f), WhaleTokens.Surface),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChannelLogo(channel = channel, size = 64.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = WhaleTokens.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = channel.displayGroupTitle("直播频道"),
                        color = WhaleTokens.SecondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Tag(label = "直播", color = WhaleTokens.Red)
                Tag(label = channel.displayGroupTitle(), color = WhaleTokens.SecondaryText)
            }
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TvTextButton(text = "立即播放", icon = Icons.Default.PlayArrow, primary = true, onClick = onPlay, modifier = Modifier.weight(1f))
                TvTextButton(
                    text = if (channel.isFavorite) "已收藏" else "收藏",
                    icon = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    focusedBorder = WhaleTokens.Gold,
                    onClick = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun ProgramTimeline(channel: TvChannel, modifier: Modifier = Modifier) {
    val programs = listOfNotNull(channel.currentProgram, channel.nextProgram)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("今日节目单", color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                DateTimeFormatter.ofPattern("yyyy年M月d日")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now()),
                color = WhaleTokens.SecondaryText,
                fontSize = 12.sp,
            )
        }
        if (programs.isEmpty()) {
            EmptyEpg(channel = channel)
        } else {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                programs.forEachIndexed { index, program ->
                    TimelineRow(
                        program = program,
                        current = index == 0 && program == channel.currentProgram,
                        last = index == programs.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(program: Program, current: Boolean, last: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.04f))
            .padding(vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.width(100.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                formatProgramTime(program.startAt),
                color = if (current) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
                fontSize = 13.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            )
            Text(formatProgramTime(program.endAt), color = Color(0xFF3A4A60), fontSize = 11.sp)
        }
        Column(
            modifier = Modifier.width(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(if (current) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(if (current) WhaleTokens.Cyan else Color(0xFF3A4A60)),
            )
            if (!last) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(52.dp)
                        .background(Color(0xFF1C2A40)),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = program.title,
                    color = if (current) WhaleTokens.PrimaryText else WhaleTokens.TertiaryText,
                    fontSize = 14.sp,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (current) {
                    LiveBadge()
                }
            }
            program.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = WhaleTokens.SecondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (current) {
                ProgramProgressBar(programProgress(program), active = true, modifier = Modifier.width(240.dp).padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptyEpg(channel: TvChannel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WhaleTokens.Surface)
            .padding(24.dp),
    ) {
        Text("暂无节目单数据", color = WhaleTokens.PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("直播功能不受影响，可继续播放 ${channel.name}", color = WhaleTokens.SecondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Tag(label: String, color: Color) {
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}
