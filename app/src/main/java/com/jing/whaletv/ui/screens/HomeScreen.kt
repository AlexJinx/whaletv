package com.jing.whaletv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.R
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.ChannelCardItem
import com.jing.whaletv.ui.HomeCategorySpec
import com.jing.whaletv.ui.HomeCountryTabSpec
import com.jing.whaletv.ui.HomeUiState
import com.jing.whaletv.ui.homeCategoryCounts
import com.jing.whaletv.ui.homeCategorySpecsForCountry
import com.jing.whaletv.ui.homeCountryId
import com.jing.whaletv.ui.homeFavoriteChannels
import com.jing.whaletv.ui.homeGridItemsForCategory
import com.jing.whaletv.ui.homeHistoryChannels
import com.jing.whaletv.ui.normalizeHomeCategoryIdForCountry
import com.jing.whaletv.ui.toChannelCardItem
import com.jing.whaletv.ui.components.FlagImage
import com.jing.whaletv.ui.components.HomeChannelCard
import com.jing.whaletv.ui.components.TvFocusStyle
import com.jing.whaletv.ui.components.TvFocusable
import com.jing.whaletv.ui.components.TvIconButton
import com.jing.whaletv.ui.components.flagResourceCode
import com.jing.whaletv.ui.theme.WhaleGradients
import com.jing.whaletv.ui.theme.WhaleShapes
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
    onEditCountries: () -> Unit,
) {
    var selectedCountry by rememberSaveable { mutableStateOf("cn") }
    var selectedCategory by rememberSaveable { mutableStateOf("all") }
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
    val visibleCategorySpecs = remember(selectedCountry) {
        homeCategorySpecsForCountry(selectedCountry)
    }
    val activeCategoryId = normalizeHomeCategoryIdForCountry(selectedCountry, selectedCategory)
    val categoryCounts = remember(countryChannels, visibleCategorySpecs) {
        homeCategoryCounts(countryChannels, visibleCategorySpecs)
    }
    val currentCategory = visibleCategorySpecs.firstOrNull { it.id == activeCategoryId }
        ?: visibleCategorySpecs.first()
    LaunchedEffect(state.countryTabs, selectedCountry) {
        if (state.countryTabs.none { it.id == selectedCountry }) {
            selectedCountry = state.countryTabs.firstOrNull()?.id ?: "cn"
        }
    }
    LaunchedEffect(selectedCountry, selectedCategory) {
        if (activeCategoryId != selectedCategory) {
            selectedCategory = activeCategoryId
        }
    }
    val selectedCountryLabel = state.countryTabs.firstOrNull { it.id == selectedCountry }?.label ?: "中国"
    val visibleItems = remember(contentMode, activeCategoryId, selectedCountry, countryChannels, allChannels) {
        when (contentMode) {
            HOME_MODE_FAVORITES -> homeFavoriteChannels(allChannels).map { it.toChannelCardItem() }
            HOME_MODE_HISTORY -> homeHistoryChannels(allChannels).map { it.toChannelCardItem() }
            else -> homeGridItemsForCategory(activeCategoryId, countryChannels)
        }
    }
    val title = when (contentMode) {
        HOME_MODE_FAVORITES -> "收藏频道"
        HOME_MODE_HISTORY -> "观看历史"
        else -> "$selectedCountryLabel · ${currentCategory.label}"
    }
    val countryFocusRequesters = remember(state.countryTabs) {
        state.countryTabs.associate { it.id to FocusRequester() }
    }
    val fallbackCountryFocusRequester = remember { FocusRequester() }
    val selectedCountryFocusRequester = countryFocusRequesters[selectedCountry]
        ?: countryFocusRequesters.values.firstOrNull()
        ?: fallbackCountryFocusRequester
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleGradients.Page),
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
            downFocusRequester = selectedCountryFocusRequester,
        )
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = WhaleTokens.Accent,
                trackColor = WhaleTokens.Surface,
            )
        }
        CountryTabBar(
            countries = state.countryTabs,
            selectedCountry = selectedCountry,
            countryFocusRequesters = countryFocusRequesters,
            onEdit = onEditCountries,
            onCountrySelected = {
                selectedCountry = it
                contentMode = HOME_MODE_BROWSE
            },
        )
        Row(modifier = Modifier.fillMaxSize()) {
            CategoryRail(
                categories = visibleCategorySpecs,
                counts = categoryCounts,
                selectedCategory = activeCategoryId,
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
    downFocusRequester: FocusRequester,
) {
    val statusText = when {
        isRefreshing -> "正在同步"
        message != null -> message
        syncSummary.playlistLastError != null -> "同步失败"
        syncSummary.playlistLastSuccessAt != null -> "已同步"
        else -> "等待同步"
    }
    val statusColor = when {
        isRefreshing -> WhaleTokens.Accent
        syncSummary.playlistLastError != null -> WhaleTokens.Red
        syncSummary.playlistLastSuccessAt != null -> WhaleTokens.Green
        else -> WhaleTokens.TextSecondary
    }
    val searchFocusRequester = remember { FocusRequester() }
    val favoritesFocusRequester = remember { FocusRequester() }
    val historyFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.whaletv_app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Text(
                    text = "鲸电视",
                    color = WhaleTokens.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(44.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(WhaleGradients.AccentBar),
            )
        }
        Spacer(Modifier.weight(1f))
        TopBarAction(
            text = "搜索",
            icon = Icons.Default.Search,
            active = false,
            onClick = onSearch,
            downFocusRequester = downFocusRequester,
            modifier = Modifier
                .focusRequester(searchFocusRequester)
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = favoritesFocusRequester
                },
        )
        Spacer(Modifier.width(32.dp))
        TopBarAction(
            text = "收藏",
            icon = Icons.Default.FavoriteBorder,
            active = activeMode == HOME_MODE_FAVORITES,
            onClick = onFavorites,
            downFocusRequester = downFocusRequester,
            modifier = Modifier
                .focusRequester(favoritesFocusRequester)
                .focusProperties {
                    left = searchFocusRequester
                    right = historyFocusRequester
                },
        )
        Spacer(Modifier.width(32.dp))
        TopBarAction(
            text = "历史",
            icon = Icons.Default.History,
            active = activeMode == HOME_MODE_HISTORY,
            onClick = onHistory,
            downFocusRequester = downFocusRequester,
            modifier = Modifier
                .focusRequester(historyFocusRequester)
                .focusProperties {
                    left = favoritesFocusRequester
                    right = settingsFocusRequester
                },
        )
        Spacer(Modifier.width(32.dp))
        TopBarAction(
            text = "设置",
            icon = Icons.Default.Settings,
            active = false,
            onClick = onSettings,
            downFocusRequester = downFocusRequester,
            modifier = Modifier
                .focusRequester(settingsFocusRequester)
                .focusProperties {
                    left = historyFocusRequester
                    right = FocusRequester.Cancel
                },
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .width(1.dp)
                .height(22.dp)
                .background(WhaleTokens.Border),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(statusColor),
        )
        Text(statusText, color = WhaleTokens.TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(start = 7.dp))
        Text(
            text = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(now)),
            color = WhaleTokens.TextPrimary,
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
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    TvFocusable(
        onClick = onClick,
        selected = active,
        shape = WhaleShapes.Button,
        modifier = modifier.onPreviewKeyEvent { event ->
            if (downFocusRequester == null || event.key != Key.DirectionDown) {
                return@onPreviewKeyEvent false
            }
            when (event.type) {
                KeyEventType.KeyDown -> {
                    downFocusRequester.requestFocus()
                    true
                }
                KeyEventType.KeyUp -> true
                else -> false
            }
        },
    ) { focused ->
        val actionColor = if (focused || active) WhaleTokens.Accent else WhaleTokens.TextSecondary
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = actionColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text,
                color = if (focused) WhaleTokens.TextPrimary else if (active) WhaleTokens.Accent else WhaleTokens.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (focused || active) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CountryTabBar(
    countries: List<HomeCountryTabSpec>,
    selectedCountry: String,
    countryFocusRequesters: Map<String, FocusRequester>,
    onEdit: () -> Unit,
    onCountrySelected: (String) -> Unit,
) {
    val editFocusRequester = remember { FocusRequester() }
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
        countries.forEachIndexed { index, country ->
            val tabFocusRequester = countryFocusRequesters.getValue(country.id)
            CountryTab(
                country = country,
                selected = selectedCountry == country.id,
                onClick = { onCountrySelected(country.id) },
                modifier = Modifier
                    .focusRequester(tabFocusRequester)
                    .focusProperties {
                        if (index == 0) {
                            left = FocusRequester.Cancel
                        }
                        if (index == countries.lastIndex) {
                            right = editFocusRequester
                        }
                    },
            )
        }
        CountryEditButton(
            onClick = onEdit,
            modifier = Modifier
                .focusRequester(editFocusRequester)
                .focusProperties { right = FocusRequester.Cancel },
        )
    }
}

@Composable
private fun CountryTab(
    country: HomeCountryTabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusable(
        onClick = onClick,
        selected = selected,
        shape = WhaleShapes.Button,
        style = TvFocusStyle(
            fillSelected = Color.Transparent,
            borderSelected = Color.Transparent,
        ),
        modifier = modifier.height(40.dp),
    ) { focused ->
        val contentColor = if (focused || selected) WhaleTokens.Accent else WhaleTokens.IconMuted
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (country.locked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            // "other" 等非国家 id 不展示旗帜，仅保留文字。
            if (flagResourceCode(country.id) != null) {
                FlagImage(
                    countryId = country.id,
                    contentDescription = null,
                    modifier = Modifier
                        .width(24.dp)
                        .height(16.dp),
                    fallbackFontSize = 9.sp,
                )
            }
            Text(
                text = country.label,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
        if (selected) {
            // matchParentSize 让指示条跟随 tab 内容宽度，不参与测量（fillMaxWidth 会把 tab 撑满整行）
            Box(Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(WhaleGradients.AccentBar),
                )
            }
        }
    }
}

@Composable
private fun CountryEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusable(
        onClick = onClick,
        shape = WhaleShapes.Button,
        modifier = modifier.height(40.dp),
    ) { focused ->
        val contentColor = if (focused) WhaleTokens.Accent else WhaleTokens.TextSecondary
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Text(
                "编辑",
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
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
    TvFocusable(
        onClick = onClick,
        selected = selected,
        shape = WhaleShapes.Card,
        style = TvFocusStyle(
            fillSelected = WhaleTokens.SurfaceRaised,
            borderSelected = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) { focused ->
        val contentColor = if (focused || selected) WhaleTokens.Accent else WhaleTokens.TextPrimary
        val iconColor = if (focused || selected) WhaleTokens.Accent else WhaleTokens.IconMuted
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(WhaleGradients.AccentBar),
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
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = category.label,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = count.toString(),
                color = if (focused || selected) WhaleTokens.Accent.copy(alpha = 0.90f) else WhaleTokens.CountDim,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(WhaleShapes.Pill)
                    .background(WhaleTokens.SurfaceRaised)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private fun HomeCategorySpec.icon(): ImageVector {
    return when (id) {
        "cctv" -> Icons.Default.Tv
        "satellite" -> Icons.Default.WifiTethering
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
    var gridFocused by remember(firstItemKey) { mutableStateOf(false) }
    var didRequestInitialGridFocus by remember { mutableStateOf(false) }
    LaunchedEffect(firstItemKey) {
        if (firstItemKey != null && !didRequestInitialGridFocus) {
            delay(120)
            runCatching { firstCardFocusRequester.requestFocus() }
            didRequestInitialGridFocus = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = WhaleTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$count 个频道",
                color = WhaleTokens.TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
            if (isGlobalMode) {
                Text(
                    text = "全局",
                    color = WhaleTokens.Accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WhaleTokens.Accent.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "最近同步 ${lastSyncAt?.let(::formatShortTime) ?: "--:--"}",
                color = WhaleTokens.TextSecondary,
                fontSize = 13.sp,
            )
            TvIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "刷新",
                onClick = onRefresh,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        if (cardItems.isEmpty()) {
            EmptyHomeState(modifier = Modifier.fillMaxSize())
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { gridFocused = it.hasFocus }
                    .focusGroup(),
            ) {
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
                            highlighted = gridFocused && item.key == highlightedCardKey,
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
            .clip(WhaleShapes.Card)
            .background(WhaleGradients.CardSurface)
            .border(1.dp, WhaleTokens.Border, WhaleShapes.Card),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = WhaleTokens.IconMuted,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = "暂无频道",
                color = WhaleTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "稍后重试或前往设置检查数据源",
                color = WhaleTokens.TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun formatShortTime(value: Long): String {
    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}

