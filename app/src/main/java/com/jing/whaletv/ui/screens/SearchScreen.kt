package com.jing.whaletv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.ChannelCardItem
import com.jing.whaletv.ui.SearchKeyboardKeys
import com.jing.whaletv.ui.components.HomeChannelCard
import com.jing.whaletv.ui.components.RequestInitialFocus
import com.jing.whaletv.ui.components.TvFocusStyle
import com.jing.whaletv.ui.components.TvFocusable
import com.jing.whaletv.ui.components.TvTextButton
import com.jing.whaletv.ui.components.WhaleTopBar
import com.jing.whaletv.ui.searchChannels
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens
import com.jing.whaletv.ui.theme.WhaleType
import com.jing.whaletv.ui.toChannelCardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    var resultItems by remember { mutableStateOf<List<ChannelCardItem>>(emptyList()) }
    // key 变化时自动取消重启，等价 150ms 防抖；空查询立即清空不延迟
    LaunchedEffect(query, channels) {
        if (query.isBlank()) {
            resultItems = emptyList()
            return@LaunchedEffect
        }
        delay(150)
        resultItems = withContext(Dispatchers.Default) {
            searchChannels(query, channels).map { it.toChannelCardItem() }
        }
    }

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

@Composable
private fun SearchTopBar(
    query: String,
    resultCount: Int,
    onBack: () -> Unit,
) {
    WhaleTopBar(
        title = "搜索",
        onBack = onBack,
        subtitle = query.ifBlank { "搜索频道名称" },
    ) {
        Text(
            text = "$resultCount 个结果",
            color = WhaleTokens.TextSecondary,
            fontSize = WhaleType.Caption,
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
            TvTextButton(
                text = "清空",
                icon = Icons.Default.Clear,
                enabled = query.isNotEmpty(),
                onClick = onClear,
            )
            Spacer(Modifier.weight(1f))
            TvTextButton(
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
            .clip(WhaleShapes.Button)
            .background(WhaleTokens.SurfaceRaised)
            .border(1.dp, WhaleTokens.Border, WhaleShapes.Button)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = WhaleTokens.Accent, modifier = Modifier.size(20.dp))
        Text(
            text = query.ifBlank { "搜索频道名称" },
            color = if (query.isBlank()) WhaleTokens.TextSecondary else WhaleTokens.TextPrimary,
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
    RequestInitialFocus(firstKeyFocusRequester)

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
    TvFocusable(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = WhaleShapes.Button,
        style = TvFocusStyle(
            fill = WhaleTokens.SurfaceRaised,
            border = WhaleTokens.Border,
        ),
    ) { focused ->
        Text(
            text = text,
            color = if (focused) WhaleTokens.Accent else WhaleTokens.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
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
    var gridFocused by remember(items.firstOrNull()?.key) { mutableStateOf(false) }
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
            Text("搜索结果", color = WhaleTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (query.isBlank()) "输入频道名称开始搜索" else "$query · ${items.size} 个频道",
                color = WhaleTokens.TextSecondary,
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { gridFocused = it.hasFocus }
                    .focusGroup(),
            ) {
                val visibleRows = if (maxHeight < 620.dp) 2 else SEARCH_GRID_VISIBLE_ROWS
                val cardHeight = ((maxHeight - SearchGridGap * (visibleRows - 1)) / visibleRows)
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
                            highlighted = gridFocused && item.key == highlightedCardKey,
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
            .clip(WhaleShapes.Item)
            .background(WhaleTokens.Surface)
            .border(1.dp, WhaleTokens.Border, WhaleShapes.Item),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = WhaleTokens.TextSecondary, fontSize = 16.sp)
    }
}
