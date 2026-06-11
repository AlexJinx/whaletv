package com.jing.whaletv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.ChannelSection
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.ui.HomeUiState
import com.jing.whaletv.ui.INTERNATIONAL_ALL_COUNTRIES
import com.jing.whaletv.ui.INTERNATIONAL_ALL_TYPES
import com.jing.whaletv.ui.currentProgress
import com.jing.whaletv.ui.currentTimeRange
import com.jing.whaletv.ui.currentTitle
import com.jing.whaletv.ui.formatShortDate
import com.jing.whaletv.ui.hasEpgData
import com.jing.whaletv.ui.internationalCountriesForChannels
import com.jing.whaletv.ui.internationalTypeBucketsForChannels
import com.jing.whaletv.ui.internationalTypeLabel
import com.jing.whaletv.ui.resolvedInternationalTypeBucket
import com.jing.whaletv.ui.logoColor
import com.jing.whaletv.ui.nextTitle
import com.jing.whaletv.ui.resolvedInternationalCountry
import com.jing.whaletv.ui.theme.WhaleTokens
import com.jing.whaletv.ui.components.ChannelCard
import com.jing.whaletv.ui.components.FocusableCard
import com.jing.whaletv.ui.components.LiveBadge
import com.jing.whaletv.ui.components.ProgramProgressBar
import com.jing.whaletv.ui.components.TvIconButton
import com.jing.whaletv.ui.components.TvTextButton
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenChannel: (TvChannel) -> Unit,
    onOpenDetail: (TvChannel) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    var activeNav by rememberSaveable { mutableStateOf("continue") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedInternationalCountry by rememberSaveable { mutableStateOf(INTERNATIONAL_ALL_COUNTRIES) }
    var selectedInternationalType by rememberSaveable { mutableStateOf(INTERNATIONAL_ALL_TYPES) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }

    val sections = state.sections
    val channels = state.channels
    val heroChannel = channels
        .filter { it.lastWatchedAt != null }
        .maxByOrNull { it.lastWatchedAt ?: 0L }
        ?: channels.firstOrNull()
    LaunchedEffect(sections) {
        if (sections.none { it.id == activeNav }) {
            activeNav = sections.firstOrNull()?.id ?: "all"
        }
    }
    val activeSection = sections.firstOrNull { it.id == activeNav }
    val baseSectionChannels = remember(activeNav, activeSection) { activeSection?.channels.orEmpty() }

    LaunchedEffect(activeSection) {
        if (activeSection == null && sections.isNotEmpty()) {
            activeNav = sections.first().id
        }
    }

    val isInternationalSection = activeNav == "international"
    val internationalCountries = remember(isInternationalSection, baseSectionChannels) {
        if (isInternationalSection) internationalCountriesForChannels(baseSectionChannels) else emptyList()
    }
    val activeInternationalCountry = if (
        isInternationalSection && selectedInternationalCountry in internationalCountries
    ) {
        selectedInternationalCountry
    } else {
        INTERNATIONAL_ALL_COUNTRIES
    }
    val countryFilteredChannels = if (!isInternationalSection || activeInternationalCountry == INTERNATIONAL_ALL_COUNTRIES) {
        baseSectionChannels
    } else {
        baseSectionChannels.filter { it.resolvedInternationalCountry() == activeInternationalCountry }
    }
    val internationalTypeBuckets = remember(isInternationalSection, countryFilteredChannels) {
        if (isInternationalSection) internationalTypeBucketsForChannels(countryFilteredChannels) else emptyList()
    }
    val activeInternationalType = if (
        isInternationalSection && selectedInternationalType in internationalTypeBuckets
    ) {
        selectedInternationalType
    } else {
        INTERNATIONAL_ALL_TYPES
    }

    val filteredChannels = if (!isInternationalSection || activeInternationalType == INTERNATIONAL_ALL_TYPES) {
        countryFilteredChannels
    } else {
        countryFilteredChannels.filter { it.resolvedInternationalTypeBucket() == activeInternationalType }
    }

    LaunchedEffect(isInternationalSection, activeInternationalCountry, activeInternationalType) {
        if (isInternationalSection) {
            if (selectedInternationalCountry != activeInternationalCountry) {
                selectedInternationalCountry = activeInternationalCountry
            }
            if (selectedInternationalType != activeInternationalType) {
                selectedInternationalType = activeInternationalType
            }
        } else {
            if (selectedInternationalCountry != INTERNATIONAL_ALL_COUNTRIES) {
                selectedInternationalCountry = INTERNATIONAL_ALL_COUNTRIES
            }
            if (selectedInternationalType != INTERNATIONAL_ALL_TYPES) {
                selectedInternationalType = INTERNATIONAL_ALL_TYPES
            }
        }
    }

    val countryChips = remember(isInternationalSection, internationalCountries, baseSectionChannels) {
        if (!isInternationalSection) emptyList() else buildList {
            val countryCountByName = baseSectionChannels
                .groupingBy { it.resolvedInternationalCountry() }
                .eachCount()
            add("$INTERNATIONAL_ALL_COUNTRIES (${baseSectionChannels.size})" to INTERNATIONAL_ALL_COUNTRIES)
            addAll(
                internationalCountries.map { country ->
                    "$country (${countryCountByName[country] ?: 0})" to country
                },
            )
        }
    }
    val typeChips = remember(isInternationalSection, internationalTypeBuckets, countryFilteredChannels) {
        if (!isInternationalSection) emptyList() else buildList {
            val typeCountByBucket = countryFilteredChannels
                .groupingBy { it.resolvedInternationalTypeBucket() }
                .eachCount()
            add("$INTERNATIONAL_ALL_TYPES (${countryFilteredChannels.size})" to INTERNATIONAL_ALL_TYPES)
            addAll(
                internationalTypeBuckets.map { bucket ->
                    "${internationalTypeLabel(bucket)} (${typeCountByBucket[bucket] ?: 0})" to bucket
                },
            )
        }
    }

    val sectionTitle = activeSection?.title ?: "全部频道"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleTokens.Background),
    ) {
        HomeTopBar(
            now = now,
            message = state.message,
            isRefreshing = state.isRefreshing,
            syncSummary = state.syncSummary,
            onSearch = onSearch,
            onSettings = onSettings,
            onRefresh = onRefresh,
        )
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = WhaleTokens.Cyan,
                trackColor = WhaleTokens.Surface,
            )
        }
        Row(modifier = Modifier.fillMaxSize()) {
            HomeNavRail(
                activeNav = activeNav,
                sections = sections,
                onSelect = { activeNav = it },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (heroChannel == null) {
                    EmptyHomeState()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        HeroCard(
                            channel = heroChannel,
                            now = now,
                            onClick = { onOpenChannel(heroChannel) },
                            modifier = Modifier.width(560.dp).height(315.dp),
                        )
                        HeroDetailPanel(
                            channel = heroChannel,
                            onPlay = { onOpenChannel(heroChannel) },
                            onDetail = { onOpenDetail(heroChannel) },
                            modifier = Modifier.weight(1f).height(315.dp),
                        )
                    }

                    if (isInternationalSection) {
                        FilterChipRow(
                            title = "国家",
                            chips = countryChips,
                            selected = activeInternationalCountry,
                            onSelect = { selectedInternationalCountry = it },
                        )
                        FilterChipRow(
                            title = "类型",
                            chips = typeChips,
                            selected = activeInternationalType,
                            onSelect = { selectedInternationalType = it },
                        )
                    }

                    ChannelRows(
                        title = sectionTitle,
                        count = filteredChannels.size,
                        channels = filteredChannels,
                        onOpenChannel = onOpenChannel,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    now: Long,
    message: String?,
    isRefreshing: Boolean,
    syncSummary: SyncSummary,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val statusText = message ?: when {
        isRefreshing -> "正在同步"
        syncSummary.playlistLastError != null -> "同步失败：${syncSummary.playlistLastError}"
        syncSummary.playlistLastSuccessAt != null -> "已同步"
        else -> "等待同步"
    }
    val statusColor = when {
        isRefreshing -> WhaleTokens.Cyan
        message?.startsWith("刷新失败") == true || message?.startsWith("同步失败") == true -> WhaleTokens.Red
        syncSummary.playlistLastError != null -> WhaleTokens.Red
        syncSummary.playlistLastSuccessAt != null -> WhaleTokens.Green
        else -> WhaleTokens.SecondaryText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(WhaleTokens.Background.copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .focusGroup()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(listOf(WhaleTokens.Cyan, Color(0xFF0080A0)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("鲸", color = WhaleTokens.Background, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .width(1.dp)
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.10f)),
        )
        Column {
            Text(
                text = DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(now)),
                color = WhaleTokens.PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatShortDate(now),
                color = WhaleTokens.SecondaryText,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp),
        )
        TvTextButton(text = "搜索频道", icon = Icons.Default.Search, onClick = onSearch)
        Spacer(Modifier.width(8.dp))
        TvIconButton(icon = Icons.Default.Refresh, contentDescription = "刷新", onClick = onRefresh)
        Spacer(Modifier.width(8.dp))
        TvIconButton(icon = Icons.Default.Settings, contentDescription = "设置", onClick = onSettings)
    }
}

@Composable
private fun HomeNavRail(
    activeNav: String,
    sections: List<ChannelSection>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(160.dp)
                .fillMaxHeight()
                .background(WhaleTokens.Sidebar)
                .border(1.dp, Color.White.copy(alpha = 0.05f))
                .padding(vertical = 16.dp)
                .focusGroup()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        sections.forEach { section ->
            val isActive = activeNav == section.id
            NavItem(
                label = section.title,
                count = section.channels.size,
                active = isActive,
                onClick = { onSelect(section.id) },
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .background(if (active || focused) Color(0x1400C8D4) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (active || focused) WhaleTokens.Cyan else Color.Transparent),
        )
        Text(
            text = label,
            color = if (active || focused) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        )
        Text(
            text = count.toString(),
            color = WhaleTokens.SecondaryText,
            fontSize = 10.sp,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

@Composable
private fun FilterChipRow(
    title: String,
    chips: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    if (chips.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = WhaleTokens.SecondaryText, fontSize = 11.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { (label, value) ->
                FilterChipItem(
                    label = label,
                    selected = selected == value,
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) WhaleTokens.Cyan.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (selected) WhaleTokens.Cyan else Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun HeroCard(
    channel: TvChannel,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTitle = channel.currentTitle()
    val nextTitle = channel.nextTitle()
    val currentTimeRange = channel.currentTimeRange()
    val currentProgress = channel.currentProgress(now)

    FocusableCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        focusedScale = 1.015f,
        container = WhaleTokens.Surface,
    ) { focused ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(channel.logoColor().copy(alpha = 0.28f), Color(0xFF0D1520), WhaleTokens.Background),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, WhaleTokens.Background), startY = 110f)),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveBadge(text = "直播中")
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = currentTimeRange ?: "直播频道",
                    color = WhaleTokens.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentTitle ?: channel.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(channel.name, color = WhaleTokens.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    nextTitle?.let {
                        Text(
                            text = "  >  下一个: $it",
                            color = WhaleTokens.SecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                currentProgress?.let {
                    ProgramProgressBar(it, active = focused, height = 3.dp)
                }
            }
            if (focused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(WhaleTokens.Cyan.copy(alpha = 0.24f))
                        .border(2.dp, WhaleTokens.Cyan, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = WhaleTokens.Cyan,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroDetailPanel(
    channel: TvChannel,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTitle = channel.currentTitle()
    val nextTitle = channel.nextTitle()
    val currentTimeRange = channel.currentTimeRange()
    val hasEpgData = channel.hasEpgData()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("继续观看", color = WhaleTokens.SecondaryText, fontSize = 11.sp, letterSpacing = 1.sp)
        Text(
            text = currentTitle ?: channel.name,
            color = WhaleTokens.PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (currentTitle == null) "直播频道" else channel.name,
            color = WhaleTokens.TertiaryText,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

        if (currentTitle != null && currentTimeRange != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(3.dp).height(28.dp).background(WhaleTokens.Cyan, RoundedCornerShape(2.dp)))
                Column {
                    Text("$currentTimeRange 正在播出", color = WhaleTokens.Cyan, fontSize = 11.sp)
                    Text(currentTitle, color = WhaleTokens.PrimaryText, fontSize = 13.sp, maxLines = 1)
                }
            }
            nextTitle?.let {
                Row(
                    modifier = Modifier.padding(start = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        Text("下一个", color = WhaleTokens.SecondaryText, fontSize = 11.sp)
                        Text(it, color = Color(0xFFB8C4D8), fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvTextButton(text = "立即播放", icon = Icons.Default.PlayArrow, primary = true, onClick = onPlay)
            TvTextButton(text = if (hasEpgData) "节目单" else "详情", onClick = onDetail)
            if (channel.isFavorite) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = WhaleTokens.Gold, modifier = Modifier.size(15.dp))
                    Text("已收藏", color = WhaleTokens.Gold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ChannelRows(
    title: String,
    count: Int,
    channels: List<TvChannel>,
    onOpenChannel: (TvChannel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("$count 个频道", color = WhaleTokens.SecondaryText, fontSize = 12.sp)
        }
        if (channels.isEmpty()) {
            Text("这个分区暂无频道", color = WhaleTokens.SecondaryText, fontSize = 13.sp)
        } else {
            val columns = 4
            val rows = channels.chunked(columns)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    }
}

@Composable
private fun EmptyHomeState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WhaleTokens.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Text("正在准备频道数据", color = WhaleTokens.SecondaryText, fontSize = 16.sp)
    }
}
