package com.jing.whaletv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.ui.ChannelCardItem
import com.jing.whaletv.ui.theme.WhaleGradients
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
fun HomeChannelCard(
    item: ChannelCardItem,
    highlighted: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val active = focused || highlighted
    val quality = item.qualityLabel
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = if (active) 8.dp else 2.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(WhaleTokens.SurfaceRaised)
            .border(
                width = 2.dp,
                color = if (active) WhaleTokens.Accent else Color.White.copy(alpha = 0.04f),
                shape = shape,
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                }
                onFocusChanged(it.isFocused)
            }
            .tvClickable(onClick = onClick),
    ) {
        ChannelLogoPanel(
            item = item,
            focused = active,
            quality = quality,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(WhaleTokens.SurfaceRaised)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = item.title,
                color = if (active) WhaleTokens.CardTitleActive else WhaleTokens.CardTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.categoryLabel,
                color = WhaleTokens.IconMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            item.currentProgramTitle?.let { current ->
                NowPlayingRow(
                    title = current,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                )
            }
            Spacer(Modifier.weight(1f))
            if (item.currentProgramTitle != null) {
                Spacer(Modifier.height(8.dp))
            }
            ChannelMetaRow(item)
        }
    }
}

@Composable
private fun ChannelLogoPanel(
    item: ChannelCardItem,
    focused: Boolean,
    quality: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(WhaleGradients.LogoPanel),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gridBrush()),
        )
        LogoBadge(item = item, focused = focused)
        quality?.let {
            Text(
                text = it,
                color = if (it == "4K" || it == "8K") WhaleTokens.Accent else WhaleTokens.CardQualityText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (it == "4K" || it == "8K") WhaleTokens.Accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f))
                    .border(
                        1.dp,
                        if (it == "4K" || it == "8K") WhaleTokens.Accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.09f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LogoBadge(item: ChannelCardItem, focused: Boolean) {
    val label = item.logoLabel
    val badgeBrush = WhaleGradients.LogoBadge
    val labelColor = when {
        label.equals("CGTN", ignoreCase = true) -> WhaleTokens.LogoTextSoft
        else -> WhaleTokens.LogoText
    }
    val labelFontSize = when {
        label.equals("CGTN", ignoreCase = true) -> 20.sp
        label.length <= 4 -> 18.sp
        label.length <= 7 -> 17.sp
        else -> 15.sp
    }

    Box(
        modifier = Modifier
            .width(108.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeBrush)
            .border(
                1.dp,
                if (focused) WhaleTokens.Accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = labelFontSize,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NowPlayingRow(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(WhaleTokens.Teal.copy(alpha = 0.09f))
            .border(1.dp, WhaleTokens.Teal.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(WhaleTokens.Teal),
        )
        Text(
            text = "正在播出：$title",
            color = WhaleTokens.LiveTealBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChannelMetaRow(item: ChannelCardItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (item.sourceCount > 0) {
            ChannelMetaChip(text = "可用", dotColor = WhaleTokens.Green, textColor = WhaleTokens.OkText)
        } else {
            ChannelMetaChip(text = "暂无可用源", dotColor = WhaleTokens.WarnDot, textColor = WhaleTokens.WarnText)
        }
        ChannelMetaChip(text = "${item.sourceCount} 个源")
        if (item.hasEpg) {
            ChannelMetaChip(text = "EPG", showCalendar = true)
        }
    }
}

@Composable
private fun ChannelMetaChip(
    text: String,
    dotColor: Color? = null,
    textColor: Color = WhaleTokens.MetaText,
    showCalendar: Boolean = false,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        dotColor?.let {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(it),
            )
        }
        if (showCalendar) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
        }
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun gridBrush(): Brush {
    return Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color.White.copy(alpha = 0.018f),
            0.48f to Color.Transparent,
            1.0f to Color.White.copy(alpha = 0.012f),
        ),
    )
}
