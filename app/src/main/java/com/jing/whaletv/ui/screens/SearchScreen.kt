package com.jing.whaletv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.ChannelCardItem
import com.jing.whaletv.ui.SearchKeyboardKeys
import com.jing.whaletv.ui.components.HomeChannelCard
import com.jing.whaletv.ui.searchChannels
import com.jing.whaletv.ui.theme.WhaleTokens
import com.jing.whaletv.ui.toChannelCardItem
import kotlinx.coroutines.delay

private const val SEARCH_GRID_VISIBLE_ROWS = 3
private val SearchGridGap = 20.dp
private val SearchKeyboardGap = 10.dp

@Composable
fun SearchScreen(
    channels: List<TvChannel>,
    onBack: () -> Unit,
    onChannelSelected: (String) -> Unit,
) {
    BackHandler(onBack = onBack)

    var query by rememberSaveable { mutableStateOf("") }
    val resultChannels = remember(query, channels) { searchChannels(query, channels) }
    val resultItems = remember(resultChannels) { resultChannels.map { it.toChannelCardItem() } }
    val platformDensity = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleTokens.Background),
        ) {
            SearchTopBar(
                query = query,
                resultCount = resultItems.size,
                onBack = onBack,
            )
            Row(modifier = Modifier.fillMaxSize()) {
                SearchInputPanel(
                    query = query,
                    onAppend = { query += it },
                    onClear = { query = "" },
                    onDelete = { query = query.dropLast(1) },
                )
                SearchResultsPane(
                    query = query,
                    items = resultItems,
                    onChannelSelected = onChannelSelected,
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    resultCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "返回", onClick = onBack)
        Icon(Icons.Default.Search, contentDescription = null, tint = WhaleTokens.Cyan, modifier = Modifier.size(24.dp))
        Text(
            text = "搜索",
            color = WhaleTokens.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
        Text(
            text = query.ifBlank { "搜索频道名称" },
            color = if (query.isBlank()) WhaleTokens.SecondaryText else WhaleTokens.PrimaryText,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 18.dp)
                .weight(1f),
        )
        Text(
            text = "$resultCount 个结果",
            color = WhaleTokens.SecondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SearchInputPanel(
    query: String,
    onAppend: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(520.dp)
            .fillMaxHeight()
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SearchQueryBox(query = query)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchActionButton(
                text = "清空",
                icon = Icons.Default.Clear,
                enabled = query.isNotEmpty(),
                onClick = onClear,
            )
            Spacer(Modifier.weight(1f))
            SearchActionButton(
                text = "删除",
                icon = Icons.AutoMirrored.Filled.Backspace,
                enabled = query.isNotEmpty(),
                onClick = onDelete,
            )
        }
        SearchKeyboard(onAppend = onAppend)
    }
}

@Composable
private fun SearchQueryBox(query: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WhaleTokens.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = WhaleTokens.Cyan, modifier = Modifier.size(20.dp))
        Text(
            text = query.ifBlank { "搜索频道名称" },
            color = if (query.isBlank()) WhaleTokens.SecondaryText else WhaleTokens.PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchKeyboard(onAppend: (String) -> Unit) {
    val firstKeyFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstKeyFocusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SearchKeyboardGap),
    ) {
        SearchKeyboardKeys.chunked(6).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SearchKeyboardGap),
            ) {
                row.forEachIndexed { keyIndex, key ->
                    val focusModifier = if (rowIndex == 0 && keyIndex == 0) {
                        Modifier.focusRequester(firstKeyFocusRequester)
                    } else {
                        Modifier
                    }
                    SearchKeyboardKey(
                        text = key,
                        onClick = { onAppend(key) },
                        modifier = focusModifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchKeyboardKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(if (focused) WhaleTokens.Cyan.copy(alpha = 0.14f) else WhaleTokens.SurfaceRaised)
            .border(
                1.dp,
                if (focused) WhaleTokens.Cyan.copy(alpha = 0.74f) else Color.White.copy(alpha = 0.07f),
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (focused) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SearchActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = focused && enabled
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .border(
                1.dp,
                if (active) WhaleTokens.Cyan.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = when {
                !enabled -> WhaleTokens.SecondaryText.copy(alpha = 0.36f)
                active -> WhaleTokens.Cyan
                else -> WhaleTokens.SecondaryText
            },
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = if (enabled) WhaleTokens.PrimaryText else WhaleTokens.SecondaryText.copy(alpha = 0.36f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SearchResultsPane(
    query: String,
    items: List<ChannelCardItem>,
    onChannelSelected: (String) -> Unit,
) {
    var highlightedCardKey by remember(items.firstOrNull()?.key) { mutableStateOf(items.firstOrNull()?.key) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleTokens.Background)
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("搜索结果", color = WhaleTokens.PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (query.isBlank()) "输入频道名称开始搜索" else "$query · ${items.size} 个频道",
                color = WhaleTokens.SecondaryText,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(20.dp))
        if (items.isEmpty()) {
            SearchEmptyState(
                text = if (query.isBlank()) "输入频道名称开始搜索" else "没有找到匹配频道",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cardHeight = ((maxHeight - SearchGridGap * (SEARCH_GRID_VISIBLE_ROWS - 1)) / SEARCH_GRID_VISIBLE_ROWS)
                    .coerceAtLeast(1.dp)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(SearchGridGap),
                    verticalArrangement = Arrangement.spacedBy(SearchGridGap),
                ) {
                    items(items, key = { it.key }) { item ->
                        HomeChannelCard(
                            item = item,
                            highlighted = item.key == highlightedCardKey,
                            onFocused = { highlightedCardKey = item.key },
                            onClick = { onChannelSelected(item.key) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WhaleTokens.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = WhaleTokens.SecondaryText, fontSize = 16.sp)
    }
}

@Composable
private fun SearchIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (focused) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}
