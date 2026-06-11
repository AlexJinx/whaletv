package com.jing.whaletv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.isPlayable
import com.jing.whaletv.ui.components.ChannelCard
import com.jing.whaletv.ui.components.ChannelLogo
import com.jing.whaletv.ui.components.LiveBadge
import com.jing.whaletv.ui.components.TvIconButton
import com.jing.whaletv.ui.currentTitle
import com.jing.whaletv.ui.displayGroupTitle
import com.jing.whaletv.ui.theme.WhaleTokens

private val recentSearches = listOf("CCTV-5", "湖南卫视", "新闻", "体育直播", "纪录片")
private val shortcuts = listOf("央视", "卫视", "体育", "新闻", "纪录", "少儿", "综合", "电影")
private val pinyinKeys = listOf(
    listOf("C", "CCTV", "H", "N", "X", "Z", "清空"),
    listOf("A", "B", "D", "F", "G", "J", "K"),
    listOf("L", "M", "P", "Q", "S", "T", "W"),
    listOf("Y", "央", "卫", "体", "新", "少", "纪"),
)

@Composable
fun SearchScreen(
    channels: List<TvChannel>,
    onBack: () -> Unit,
    onOpenChannel: (TvChannel) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = if (query.isBlank()) {
        emptyList()
    } else {
        channels.filter {
            val matchesQuery = it.name.contains(query, ignoreCase = true) ||
                it.groupTitle.contains(query, ignoreCase = true) ||
                it.displayGroupTitle().contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true) ||
                it.currentTitle()?.contains(query, ignoreCase = true) == true
            it.isPlayable() && matchesQuery
        }.take(48)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleTokens.Background),
    ) {
        SearchHeader(onBack)
        Row(modifier = Modifier.fillMaxSize()) {
            SearchInputPanel(
                query = query,
                onQueryChange = { query = it },
                onAppend = { value -> query += value },
                onClear = { query = "" },
            )
            SearchResultsPanel(
                query = query,
                results = results,
                channels = channels,
                onSetQuery = { query = it },
                onOpenChannel = onOpenChannel,
            )
        }
    }
}

@Composable
private fun SearchHeader(onBack: () -> Unit) {
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
        Text("搜索频道", color = WhaleTokens.PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SearchInputPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    onAppend: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(420.dp)
            .fillMaxHeight()
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WhaleTokens.Surface)
                .border(1.dp, WhaleTokens.Cyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .shadow(12.dp, RoundedCornerShape(8.dp), ambientColor = WhaleTokens.Cyan.copy(alpha = 0.12f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = WhaleTokens.Cyan, modifier = Modifier.size(20.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = query.ifBlank { "输入频道名称或拼音..." },
                    color = if (query.isBlank()) Color(0xFF3A4A60) else WhaleTokens.PrimaryText,
                    fontSize = if (query.isBlank()) 16.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .padding(start = 3.dp)
                        .width(2.dp)
                        .height(20.dp)
                        .background(WhaleTokens.Cyan),
                )
            }
            if (query.isNotBlank()) {
                TvIconButton(icon = Icons.Default.Close, contentDescription = "清空", onClick = onClear)
            }
        }

        Label("快速选择")
        ChipRows(shortcuts, onClick = onAppend)

        Label("拼音输入")
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            pinyinKeys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { key ->
                        KeyboardKey(
                            text = key,
                            danger = key == "清空",
                            modifier = Modifier.weight(if (key == "CCTV") 1.45f else 1f),
                            onClick = {
                                if (key == "清空") {
                                    onClear()
                                } else {
                                    onQueryChange(query + key)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsPanel(
    query: String,
    results: List<TvChannel>,
    channels: List<TvChannel>,
    onSetQuery: (String) -> Unit,
    onOpenChannel: (TvChannel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        if (query.isBlank()) {
            DefaultSearchState(
                channels = channels,
                onSetQuery = onSetQuery,
                onOpenChannel = onOpenChannel,
            )
        } else {
            Text(
                text = "找到 ${results.size} 个频道",
                color = WhaleTokens.SecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            SearchChannelGrid(
                channels = results,
                onOpenChannel = onOpenChannel,
            )
        }
    }
}

@Composable
private fun SearchChannelGrid(
    channels: List<TvChannel>,
    onOpenChannel: (TvChannel) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val columns = 4
        val rows = channels.chunked(columns)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onOpenChannel(channel) },
                        focusedScale = 1f,
                        modifier = Modifier
                            .width(0.dp)
                            .weight(1f)
                            .heightIn(max = 124.dp)
                            .height(124.dp),
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DefaultSearchState(
    channels: List<TvChannel>,
    onSetQuery: (String) -> Unit,
    onOpenChannel: (TvChannel) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        val trending = channels
            .filter { it.isFavorite || it.name.contains("CCTV") || it.name.contains("卫视") }
            .filter { it.isPlayable() }
            .take(12)

        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = WhaleTokens.SecondaryText, modifier = Modifier.size(16.dp))
                Text("最近搜索", color = WhaleTokens.SecondaryText, fontSize = 11.sp, letterSpacing = 1.sp)
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recentSearches.forEach { item ->
                    SearchChip(text = item, onClick = { onSetQuery(item) })
                }
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, tint = WhaleTokens.SecondaryText, modifier = Modifier.size(16.dp))
                Text("热门频道", color = WhaleTokens.SecondaryText, fontSize = 11.sp, letterSpacing = 1.sp)
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                trending.take(6).forEach { channel ->
                    TrendingChannel(channel = channel, onClick = { onOpenChannel(channel) })
                }
            }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                trending.drop(6).take(6).forEach { channel ->
                    TrendingChannel(channel = channel, onClick = { onOpenChannel(channel) })
                }
            }
        }
    }
}

@Composable
private fun SearchChip(text: String, onClick: () -> Unit) {
    FocusableSmallSurface(onClick = onClick) { focused ->
        Text(
            text = text,
            color = if (focused) WhaleTokens.Cyan else WhaleTokens.TertiaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ChipRows(values: List<String>, onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { value ->
                    SearchChip(text = value, onClick = { onClick(value) })
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String,
    danger: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableSmallSurface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        danger = danger,
    ) { focused ->
        Text(
            text = text,
            color = when {
                danger -> if (focused) WhaleTokens.Red else WhaleTokens.SecondaryText
                focused -> WhaleTokens.Cyan
                else -> Color(0xFFB8C4D8)
            },
            fontSize = if (danger) 11.sp else 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TrendingChannel(channel: TvChannel, onClick: () -> Unit) {
    FocusableSmallSurface(
        onClick = onClick,
        modifier = Modifier.width(180.dp).height(72.dp),
    ) {
        val currentTitle = channel.currentTitle()
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChannelLogo(channel = channel, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = WhaleTokens.PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentTitle ?: channel.displayGroupTitle("直播频道"),
                    color = WhaleTokens.SecondaryText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    LiveBadge(compact = true)
                }
            }
        }
    }
}

@Composable
private fun FocusableSmallSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    content: @Composable (Boolean) -> Unit,
) {
    var focused by rememberSaveable { mutableStateOf(false) }
    val color = if (danger) WhaleTokens.Red else WhaleTokens.Cyan
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (focused) 1.05f else 1f
                scaleY = if (focused) 1.05f else 1f
            }
            .clip(RoundedCornerShape(5.dp))
            .background(if (focused) WhaleTokens.SurfaceRaised else WhaleTokens.Surface)
            .border(1.dp, if (focused) color else Color.White.copy(alpha = 0.07f), RoundedCornerShape(5.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(focused)
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        color = WhaleTokens.SecondaryText,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
