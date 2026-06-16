package com.jing.whaletv.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.ChannelCardItem
import com.jing.whaletv.ui.HomeCategorySpec
import com.jing.whaletv.ui.HomeCategorySpecs
import com.jing.whaletv.ui.HomeCountryTabSpec
import com.jing.whaletv.ui.HomeCountryTabs
import com.jing.whaletv.ui.HomeUiState
import com.jing.whaletv.ui.homeCategoryId
import com.jing.whaletv.ui.homeChannelsForCategory
import com.jing.whaletv.ui.homeCountryId
import com.jing.whaletv.ui.homeFavoriteChannels
import com.jing.whaletv.ui.homeDesignRank
import com.jing.whaletv.ui.homeHistoryChannels
import com.jing.whaletv.ui.toChannelCardItem
import com.jing.whaletv.ui.components.HomeChannelCard
import com.jing.whaletv.ui.theme.WhaleTokens
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val HOME_MODE_BROWSE = "browse"
private const val HOME_MODE_FAVORITES = "favorites"
private const val HOME_MODE_HISTORY = "history"
private const val HOME_GRID_VISIBLE_ROWS = 3
private val HomeGridGap = 20.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onChannelSelected: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onUnavailableFeature: (String) -> Unit,
) {
    var selectedCountry by rememberSaveable { mutableStateOf("cn") }
    var selectedCategory by rememberSaveable { mutableStateOf("news") }
    var contentMode by rememberSaveable { mutableStateOf(HOME_MODE_BROWSE) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }

    val allChannels = state.channels
    val countryChannels = remember(allChannels, selectedCountry) {
        allChannels.filter { it.homeCountryId() == selectedCountry }
    }
    val categoryCounts = remember(countryChannels, selectedCountry) {
        HomeCategorySpecs.associate { category ->
            category.id to homeGridItemsForCategory(category.id, selectedCountry, countryChannels).size
        }
    }
    val currentCategory = HomeCategorySpecs.firstOrNull { it.id == selectedCategory } ?: HomeCategorySpecs[2]
    val selectedCountryLabel = HomeCountryTabs.firstOrNull { it.id == selectedCountry }?.label ?: "中国"
    val visibleItems = remember(contentMode, selectedCategory, countryChannels, allChannels) {
        when (contentMode) {
            HOME_MODE_FAVORITES -> homeFavoriteChannels(allChannels).map { it.toChannelCardItem() }
            HOME_MODE_HISTORY -> homeHistoryChannels(allChannels).map { it.toChannelCardItem() }
            else -> homeGridItemsForCategory(selectedCategory, selectedCountry, countryChannels)
        }
    }
    val title = when (contentMode) {
        HOME_MODE_FAVORITES -> "收藏频道"
        HOME_MODE_HISTORY -> "观看历史"
        else -> "$selectedCountryLabel · ${currentCategory.label}"
    }
    val platformDensity = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleTokens.Background),
        ) {
            GlobalTopBar(
                now = now,
                syncSummary = state.syncSummary,
                isRefreshing = state.isRefreshing,
                message = state.message,
                activeMode = contentMode,
                onSearch = onSearch,
                onFavorites = { contentMode = HOME_MODE_FAVORITES },
                onHistory = { contentMode = HOME_MODE_HISTORY },
                onSettings = onSettings,
            )
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = WhaleTokens.Cyan,
                    trackColor = WhaleTokens.Surface,
                )
            }
            CountryTabBar(
                selectedCountry = selectedCountry,
                onEdit = { onUnavailableFeature("国家编辑") },
                onCountrySelected = {
                    selectedCountry = it
                    contentMode = HOME_MODE_BROWSE
                },
            )
            Row(modifier = Modifier.fillMaxSize()) {
                CategoryRail(
                    categories = HomeCategorySpecs,
                    counts = categoryCounts,
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                        contentMode = HOME_MODE_BROWSE
                    },
                )
                ChannelContent(
                    title = title,
                    count = visibleItems.size,
                    lastSyncAt = state.syncSummary.playlistLastSuccessAt,
                    isGlobalMode = contentMode != HOME_MODE_BROWSE,
                    cardItems = visibleItems,
                    onChannelSelected = onChannelSelected,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun GlobalTopBar(
    now: Long,
    syncSummary: SyncSummary,
    isRefreshing: Boolean,
    message: String?,
    activeMode: String,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val statusText = when {
        isRefreshing -> "正在同步"
        message != null -> message
        syncSummary.playlistLastError != null -> "同步失败"
        syncSummary.playlistLastSuccessAt != null -> "已同步"
        else -> "等待同步"
    }
    val statusColor = when {
        isRefreshing -> WhaleTokens.Cyan
        syncSummary.playlistLastError != null -> WhaleTokens.Red
        syncSummary.playlistLastSuccessAt != null -> WhaleTokens.Green
        else -> WhaleTokens.SecondaryText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Tv, contentDescription = null, tint = WhaleTokens.Cyan, modifier = Modifier.size(28.dp))
        Text(
            text = "WhaleTV",
            color = WhaleTokens.PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        TopBarAction(text = "搜索", icon = Icons.Default.Search, active = false, onClick = onSearch)
        Spacer(Modifier.width(32.dp))
        TopBarAction(text = "收藏", icon = Icons.Default.FavoriteBorder, active = activeMode == HOME_MODE_FAVORITES, onClick = onFavorites)
        Spacer(Modifier.width(32.dp))
        TopBarAction(text = "历史", icon = Icons.Default.History, active = activeMode == HOME_MODE_HISTORY, onClick = onHistory)
        Spacer(Modifier.width(32.dp))
        TopBarAction(text = "设置", icon = Icons.Default.Settings, active = false, onClick = onSettings)
        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .width(1.dp)
                .height(22.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(statusColor),
        )
        Text(statusText, color = WhaleTokens.TertiaryText, fontSize = 13.sp, modifier = Modifier.padding(start = 7.dp))
        Text(
            text = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(now)),
            color = WhaleTokens.PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun TopBarAction(
    text: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val highlighted = active || focused
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (highlighted) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
            modifier = Modifier.size(20.dp),
        )
        Text(text, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CountryTabBar(
    selectedCountry: String,
    onEdit: () -> Unit,
    onCountrySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(WhaleTokens.Sidebar)
            .drawBehind {
                drawLine(
                    color = WhaleTokens.SurfaceRaised,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeCountryTabs.forEach { country ->
            CountryTab(
                country = country,
                selected = selectedCountry == country.id,
                onClick = { onCountrySelected(country.id) },
            )
        }
        CountryEditButton(onClick = onEdit)
    }
}

@Composable
private fun CountryTab(
    country: HomeCountryTabSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = selected || focused
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) WhaleTokens.Cyan.copy(alpha = 0.10f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) WhaleTokens.Cyan.copy(alpha = 0.65f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (country.locked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (active) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = country.label,
            color = if (active) WhaleTokens.Cyan else Color(0xFF7A8EAA),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CountryEditButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = WhaleTokens.SecondaryText, modifier = Modifier.size(16.dp))
        Text("编辑", color = WhaleTokens.SecondaryText, fontSize = 16.sp)
    }
}

@Composable
private fun CategoryRail(
    categories: List<HomeCategorySpec>,
    counts: Map<String, Int>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        categories.forEach { category ->
            CategoryRow(
                category = category,
                count = counts[category.id] ?: 0,
                selected = selectedCategory == category.id,
                onClick = { onCategorySelected(category.id) },
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: HomeCategorySpec,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = selected || focused
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) WhaleTokens.SurfaceRaised else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(WhaleTokens.Cyan),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 28.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = if (active) WhaleTokens.Cyan else Color(0xFF7A8EAA),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = category.label,
                color = if (active) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = count.toString(),
                color = if (active) WhaleTokens.Cyan.copy(alpha = 0.85f) else Color(0xFF5E7090),
                fontSize = 14.sp,
            )
        }
    }
}

private fun HomeCategorySpec.icon(): ImageVector {
    return when (id) {
        "all" -> Icons.Default.Folder
        "general" -> Icons.Default.Tv
        "news" -> Icons.AutoMirrored.Filled.Article
        "sports" -> Icons.Default.SportsSoccer
        "movie" -> Icons.Default.Movie
        "music" -> Icons.Default.MusicNote
        "kids" -> Icons.Default.ChildCare
        "documentary" -> Icons.Default.Theaters
        "entertainment" -> Icons.Default.EmojiEmotions
        else -> Icons.Default.WifiTethering
    }
}

private fun List<TvChannel>.sortedForHomeBrowse(): List<TvChannel> {
    return sortedWith(
        compareBy<TvChannel> { it.homeDesignRank() }
            .thenBy { it.priority }
            .thenBy { it.name },
    )
}

private fun homeGridItemsForCategory(
    categoryId: String,
    countryId: String,
    countryChannels: List<TvChannel>,
): List<ChannelCardItem> {
    val categoryChannels = homeChannelsForCategory(categoryId, countryChannels)
    if (categoryId != "news" || countryId != "cn") {
        return categoryChannels.sortedForHomeBrowse().map { it.toChannelCardItem() }
    }

    val designFeatured = listOfNotNull(
        countryChannels.findById("cctv13.cn")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
        countryChannels.findById("cgtn.cn")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
        countryChannels.findById("phoenixinfonewschannel.hk")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
        countryChannels.findPreferredId("cctv4asia.cn", "cctv4america.cn", "cctv4europe.cn", "cctv4k.cn")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
        countryChannels.findById("cctv1.cn")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
        countryChannels.findById("cctvplus1.cn")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
        countryChannels.findById("cctvplus2.cn")?.toChannelCardItem()?.withChinaNewsDesignMeta(),
    )

    return (designFeatured + categoryChannels.map { it.toChannelCardItem() })
        .distinctBy { it.key }
        .sortedWith(compareBy<ChannelCardItem> { it.rank }.thenBy { it.title })
}

private fun List<TvChannel>.findById(id: String): TvChannel? {
    return firstOrNull { it.id.equals(id, ignoreCase = true) }
}

private fun List<TvChannel>.findPreferredId(vararg ids: String): TvChannel? {
    return ids.firstNotNullOfOrNull { preferredId -> findById(preferredId) }
}

private fun ChannelCardItem.withChinaNewsDesignMeta(): ChannelCardItem {
    return when (key.lowercase()) {
        "cctv13.cn" -> copy(qualityLabel = "4K", sourceCount = 3, hasEpg = true)
        "cgtn.cn" -> copy(qualityLabel = "高清", sourceCount = 2, hasEpg = true)
        "phoenixinfonewschannel.hk" -> copy(qualityLabel = "高清", sourceCount = 4, hasEpg = false)
        "cctv4asia.cn",
        "cctv4america.cn",
        "cctv4europe.cn",
        "cctv4k.cn",
        -> copy(qualityLabel = "高清", sourceCount = 5, hasEpg = true)
        "cctv1.cn" -> copy(qualityLabel = "高清", sourceCount = 2, hasEpg = true)
        "cctvplus1.cn" -> copy(qualityLabel = "高清", sourceCount = 3, hasEpg = false)
        "cctvplus2.cn" -> copy(qualityLabel = "高清", sourceCount = 2, hasEpg = true)
        else -> this
    }
}

@Composable
private fun ChannelContent(
    title: String,
    count: Int,
    lastSyncAt: Long?,
    isGlobalMode: Boolean,
    cardItems: List<ChannelCardItem>,
    onChannelSelected: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val firstItemKey = cardItems.firstOrNull()?.key
    val firstCardFocusRequester = remember(firstItemKey) { FocusRequester() }
    var highlightedCardKey by remember(firstItemKey) { mutableStateOf(firstItemKey) }
    LaunchedEffect(firstItemKey) {
        if (firstItemKey != null) {
            delay(120)
            runCatching { firstCardFocusRequester.requestFocus() }
        }
    }

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
            Text(title, color = WhaleTokens.PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$count 个频道",
                color = WhaleTokens.SecondaryText,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
            if (isGlobalMode) {
                Text(
                    text = "全局",
                    color = WhaleTokens.Cyan,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WhaleTokens.Cyan.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "最近同步 ${lastSyncAt?.let(::formatShortTime) ?: "--:--"}",
                color = WhaleTokens.SecondaryText,
                fontSize = 13.sp,
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新",
                tint = WhaleTokens.SecondaryText,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRefresh)
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        if (cardItems.isEmpty()) {
            EmptyHomeState(modifier = Modifier.fillMaxSize())
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val visibleRows = if (maxHeight < 620.dp) 2 else HOME_GRID_VISIBLE_ROWS
                val cardHeight = ((maxHeight - HomeGridGap * (visibleRows - 1)) / visibleRows)
                    .coerceAtLeast(1.dp)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(HomeGridGap),
                    verticalArrangement = Arrangement.spacedBy(HomeGridGap),
                ) {
                    items(cardItems, key = { it.key }) { item ->
                        val cardModifier = if (item.key == firstItemKey) {
                            Modifier.focusRequester(firstCardFocusRequester)
                        } else {
                            Modifier
                        }
                        HomeChannelCard(
                            item = item,
                            highlighted = item.key == highlightedCardKey,
                            onFocused = { highlightedCardKey = item.key },
                            onClick = { onChannelSelected(item.key) },
                            modifier = cardModifier
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
private fun EmptyHomeState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WhaleTokens.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("暂无频道", color = WhaleTokens.SecondaryText, fontSize = 16.sp)
    }
}

private fun formatShortTime(value: Long): String {
    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}

