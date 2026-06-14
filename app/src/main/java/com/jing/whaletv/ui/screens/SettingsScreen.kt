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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.repository.SettingsRepository
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    settings: AppSettings,
    syncSummary: SyncSummary,
    diagnostics: SettingsDiagnostics,
    effectiveEpgUrl: String?,
    isRefreshing: Boolean,
    message: String?,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onRefreshNow: () -> Unit,
    onTestDefaultPlaylistSource: () -> Unit,
    onTestActiveEpgSource: () -> Unit,
    onResetStreamHealth: () -> Unit,
    onClearEpgCache: () -> Unit,
    onClearWatchHistory: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var selectedMenuId by rememberSaveable { mutableStateOf(SettingsMenu.Source.name) }
    var autoRefresh by rememberSaveable { mutableStateOf(settings.autoRefresh) }
    var refreshIntervalText by rememberSaveable { mutableStateOf(settings.refreshIntervalHours.toString()) }
    var pendingMaintenanceAction by rememberSaveable { mutableStateOf<String?>(null) }
    val refreshInterval = refreshIntervalText.toIntOrNull()
        ?.let(SettingsRepository::normalizeRefreshIntervalHours)
        ?: AppConstants.DEFAULT_REFRESH_INTERVAL_HOURS
    val draftSettings = AppSettings(
        autoRefresh = autoRefresh,
        refreshIntervalHours = refreshInterval,
    )
    val normalizedSettings = settings.copy(
        refreshIntervalHours = SettingsRepository.normalizeRefreshIntervalHours(settings.refreshIntervalHours),
    )
    val hasUnsavedChanges = draftSettings != normalizedSettings
    val selectedMenu = SettingsMenu.values().firstOrNull { it.name == selectedMenuId } ?: SettingsMenu.Source
    val menuSpecs = remember { settingsMenuSpecs() }
    val selectedSpec = menuSpecs.first { it.menu == selectedMenu }

    LaunchedEffect(settings) {
        autoRefresh = settings.autoRefresh
        refreshIntervalText = settings.refreshIntervalHours.toString()
    }

    val platformDensity = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleTokens.Background),
        ) {
            SettingsTopBar(
                isRefreshing = isRefreshing,
                onBack = onBack,
                onSave = { onSave(draftSettings) },
                onRefreshNow = onRefreshNow,
                hasUnsavedChanges = hasUnsavedChanges,
            )
            SettingsContextBar(
                selectedSpec = selectedSpec,
            )
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsSidebar(
                    specs = menuSpecs,
                    selected = selectedMenu,
                    onSelected = { selectedMenuId = it.name },
                )
                SettingsContentPane(
                    selectedSpec = selectedSpec,
                    syncSummary = syncSummary,
                    diagnostics = diagnostics,
                    effectiveEpgUrl = effectiveEpgUrl,
                    isRefreshing = isRefreshing,
                    message = message,
                    hasUnsavedChanges = hasUnsavedChanges,
                    autoRefresh = autoRefresh,
                    onAutoRefreshChange = { autoRefresh = it },
                    refreshInterval = refreshInterval,
                    refreshIntervalText = refreshIntervalText,
                    onRefreshIntervalTextChange = { refreshIntervalText = it.filter(Char::isDigit).take(2) },
                    onRefreshIntervalStep = { delta ->
                        refreshIntervalText = SettingsRepository
                            .normalizeRefreshIntervalHours(refreshInterval + delta)
                            .toString()
                    },
                    onTestDefaultPlaylistSource = onTestDefaultPlaylistSource,
                    onTestActiveEpgSource = onTestActiveEpgSource,
                    pendingMaintenanceAction = pendingMaintenanceAction,
                    onMaintenanceActionPending = { pendingMaintenanceAction = it },
                    onResetStreamHealth = {
                        pendingMaintenanceAction = null
                        onResetStreamHealth()
                    },
                    onClearEpgCache = {
                        pendingMaintenanceAction = null
                        onClearEpgCache()
                    },
                    onClearWatchHistory = {
                        pendingMaintenanceAction = null
                        onClearWatchHistory()
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsContextBar(
    selectedSpec: SettingsMenuSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "本地设置",
            color = WhaleTokens.PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "播放源 · 节目单 · 自动刷新",
            color = WhaleTokens.SecondaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "当前 ${selectedSpec.title}",
            color = WhaleTokens.Cyan,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(WhaleTokens.Cyan.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SettingsTopBar(
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onRefreshNow: () -> Unit,
    hasUnsavedChanges: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "返回", onClick = onBack)
            Icon(Icons.Default.Settings, contentDescription = null, tint = WhaleTokens.Cyan, modifier = Modifier.size(28.dp))
            Text(
                text = "设置",
                color = WhaleTokens.PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            SettingsTopBarAction(text = "立即刷新", icon = Icons.Default.Refresh, enabled = !isRefreshing, onClick = onRefreshNow)
            Spacer(Modifier.width(32.dp))
            SettingsTopBarAction(text = "保存", icon = Icons.Default.Save, highlighted = hasUnsavedChanges, onClick = onSave)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(WhaleTokens.SurfaceRaised),
        )
    }
}

@Composable
private fun SettingsSidebar(
    specs: List<SettingsMenuSpec>,
    selected: SettingsMenu,
    onSelected: (SettingsMenu) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(WhaleTokens.Sidebar)
            .padding(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        specs.forEach { spec ->
            SettingsMenuButton(
                spec = spec,
                selected = spec.menu == selected,
                onClick = { onSelected(spec.menu) },
            )
        }
    }
}

@Composable
private fun SettingsMenuButton(
    spec: SettingsMenuSpec,
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
                spec.icon,
                contentDescription = null,
                tint = if (active) WhaleTokens.Cyan else Color(0xFF7A8EAA),
                modifier = Modifier.size(20.dp),
            )
            Text(
                spec.title,
                color = if (active) WhaleTokens.Cyan else WhaleTokens.PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingsContentPane(
    selectedSpec: SettingsMenuSpec,
    syncSummary: SyncSummary,
    diagnostics: SettingsDiagnostics,
    effectiveEpgUrl: String?,
    isRefreshing: Boolean,
    message: String?,
    hasUnsavedChanges: Boolean,
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    refreshInterval: Int,
    refreshIntervalText: String,
    onRefreshIntervalTextChange: (String) -> Unit,
    onRefreshIntervalStep: (Int) -> Unit,
    onTestDefaultPlaylistSource: () -> Unit,
    onTestActiveEpgSource: () -> Unit,
    pendingMaintenanceAction: String?,
    onMaintenanceActionPending: (String) -> Unit,
    onResetStreamHealth: () -> Unit,
    onClearEpgCache: () -> Unit,
    onClearWatchHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContentHeader(
            spec = selectedSpec,
            status = settingsHeaderStatus(
                isRefreshing = isRefreshing,
                hasUnsavedChanges = hasUnsavedChanges,
                message = message,
            ),
        )
        when (selectedSpec.menu) {
            SettingsMenu.Source -> SourceSettingsContent(
                effectiveEpgUrl = effectiveEpgUrl,
                onTestDefaultPlaylistSource = onTestDefaultPlaylistSource,
                onTestActiveEpgSource = onTestActiveEpgSource,
            )
            SettingsMenu.Epg -> EpgSettingsContent(
                effectiveEpgUrl = effectiveEpgUrl,
                discoveredEpgUrl = syncSummary.discoveredEpgUrl,
                onTestActiveEpgSource = onTestActiveEpgSource,
            )
            SettingsMenu.Refresh -> RefreshSettingsContent(
                autoRefresh = autoRefresh,
                onAutoRefreshChange = onAutoRefreshChange,
                refreshInterval = refreshInterval,
                refreshIntervalText = refreshIntervalText,
                onRefreshIntervalTextChange = onRefreshIntervalTextChange,
                onRefreshIntervalStep = onRefreshIntervalStep,
            )
            SettingsMenu.Status -> SyncStatusContent(
                syncSummary = syncSummary,
                diagnostics = diagnostics,
                isRefreshing = isRefreshing,
            )
            SettingsMenu.Maintenance -> MaintenanceSettingsContent(
                diagnostics = diagnostics,
                pendingAction = pendingMaintenanceAction,
                onPendingAction = onMaintenanceActionPending,
                onResetStreamHealth = onResetStreamHealth,
                onClearEpgCache = onClearEpgCache,
                onClearWatchHistory = onClearWatchHistory,
            )
            SettingsMenu.About -> AboutSourcesContent()
        }
    }
}

@Composable
private fun ContentHeader(spec: SettingsMenuSpec, status: StatusVisual) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(spec.title, color = WhaleTokens.PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = spec.longDescription,
            color = WhaleTokens.SecondaryText,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        StatusPill(text = status.text, color = status.color)
    }
}

@Composable
private fun SourceSettingsContent(
    effectiveEpgUrl: String?,
    onTestDefaultPlaylistSource: () -> Unit,
    onTestActiveEpgSource: () -> Unit,
) {
    SettingsCardGrid { cardWidth ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SettingsGridGap)) {
                SourceStatusCard(
                    title = "默认 playlist",
                    note = "iptv-org 开源频道索引",
                    url = AppConstants.PRIMARY_PLAYLIST_URL,
                    enabled = true,
                    actionText = "测试源",
                    onAction = onTestDefaultPlaylistSource,
                    modifier = Modifier.width(cardWidth),
                )
                SourceStatusCard(
                    title = "当前 EPG",
                    note = "来自 playlist 自动发现",
                    url = effectiveEpgUrl ?: "尚未发现 x-tvg-url",
                    enabled = !effectiveEpgUrl.isNullOrBlank(),
                    actionText = "测试节目单",
                    onAction = onTestActiveEpgSource,
                    modifier = Modifier.width(cardWidth),
                )
                SettingsValueCard(
                    title = "EPG / API",
                    description = "开源节目单与元数据",
                    value = "github.com/iptv-org/epg · github.com/iptv-org/api",
                    modifier = Modifier.width(cardWidth),
                )
                SettingsTextCard(
                    title = "数据策略",
                    description = "频道和节目单只使用开源项目默认来源，不再提供手动添加数据源。",
                    modifier = Modifier.width(cardWidth),
                )
            }
            SettingsHintText("所有频道数据来自开源项目；如果需要缩小国家、语言或分类范围，后续单独做官方源范围选择器。")
        }
    }
}

@Composable
private fun EpgSettingsContent(
    effectiveEpgUrl: String?,
    discoveredEpgUrl: String?,
    onTestActiveEpgSource: () -> Unit,
) {
    SettingsCardGrid { cardWidth ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SettingsGridGap)) {
                SourceStatusCard(
                    title = "当前生效 EPG",
                    note = when {
                        !discoveredEpgUrl.isNullOrBlank() -> "来自 playlist 自动发现"
                        else -> "尚未发现节目单地址"
                    },
                    url = effectiveEpgUrl ?: "playlist 暂未发现 x-tvg-url",
                    enabled = !effectiveEpgUrl.isNullOrBlank(),
                    actionText = "测试节目单",
                    onAction = onTestActiveEpgSource,
                    modifier = Modifier.width(cardWidth),
                )
                SettingsValueCard(
                    title = "发现到的地址",
                    description = "playlist x-tvg-url",
                    value = discoveredEpgUrl ?: "等待下一次同步发现",
                    modifier = Modifier.width(cardWidth),
                )
                SettingsTextCard(
                    title = "节目展示",
                    description = "播放器和首页的当前节目、下一节目都来自这个开源节目单。",
                    modifier = Modifier.width(cardWidth),
                )
                SettingsTextCard(
                    title = "同步方式",
                    description = "立即刷新或自动刷新会同步频道后再尝试更新节目单。",
                    modifier = Modifier.width(cardWidth),
                )
            }
            SettingsHintText("没有发现 x-tvg-url 时，节目单页不会显示手动输入框；请先刷新默认 playlist。")
        }
    }
}

@Composable
private fun RefreshSettingsContent(
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    refreshInterval: Int,
    refreshIntervalText: String,
    onRefreshIntervalTextChange: (String) -> Unit,
    onRefreshIntervalStep: (Int) -> Unit,
) {
    SettingsCardGrid { cardWidth ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SettingsGridGap)) {
                SettingsSwitchCard(
                    title = "自动刷新",
                    description = "按间隔同步 playlist 与 EPG",
                    checked = autoRefresh,
                    onCheckedChange = onAutoRefreshChange,
                    modifier = Modifier.width(cardWidth),
                )
                SettingsIntervalCard(
                    value = refreshInterval,
                    text = refreshIntervalText,
                    enabled = autoRefresh,
                    onTextChange = onRefreshIntervalTextChange,
                    onStep = onRefreshIntervalStep,
                    modifier = Modifier.width(cardWidth),
                )
            }
            SettingsHintText("自动刷新关闭后，顶部的立即刷新仍然可以手动同步频道和节目单。")
        }
    }
}

@Composable
private fun SyncStatusContent(
    syncSummary: SyncSummary,
    diagnostics: SettingsDiagnostics,
    isRefreshing: Boolean,
) {
    SettingsCardGrid { cardWidth ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SettingsGridGap)) {
                SettingsStatusCard(
                    label = "Playlist",
                    description = "${diagnostics.playableChannelCount}/${diagnostics.channelCount} 个频道可播放 · ${diagnostics.streamCount} 个源",
                    lastAttemptAt = syncSummary.playlistLastAttemptAt,
                    lastSuccessAt = syncSummary.playlistLastSuccessAt,
                    error = syncSummary.playlistLastError,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.width(cardWidth),
                )
                SettingsStatusCard(
                    label = "EPG",
                    description = "${diagnostics.programCount} 条节目单 · ${if (syncSummary.discoveredEpgUrl.isNullOrBlank()) "未发现自动地址" else "已发现自动地址"}",
                    lastAttemptAt = syncSummary.epgLastAttemptAt,
                    lastSuccessAt = syncSummary.epgLastSuccessAt,
                    error = syncSummary.epgLastError,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.width(cardWidth),
                )
                SettingsMetricCard(
                    title = "用户数据",
                    value = "${diagnostics.favoriteCount} 收藏 · ${diagnostics.historyCount} 历史",
                    description = "${diagnostics.unhealthyStreamCount} 个播放源标记不可用",
                    modifier = Modifier.width(cardWidth),
                )
            }
            SettingsHintText("最近尝试时间表示系统上一次启动同步任务；最近成功时间表示数据真正更新或确认未变化。")
        }
    }
}

@Composable
private fun MaintenanceSettingsContent(
    diagnostics: SettingsDiagnostics,
    pendingAction: String?,
    onPendingAction: (String) -> Unit,
    onResetStreamHealth: () -> Unit,
    onClearEpgCache: () -> Unit,
    onClearWatchHistory: () -> Unit,
) {
    SettingsCardGrid { cardWidth ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SettingsGridGap)) {
                MaintenanceActionCard(
                    title = "重置播放源健康",
                    description = "${diagnostics.unhealthyStreamCount} 个源当前标记不可用",
                    actionId = MAINTENANCE_RESET_STREAMS,
                    pendingAction = pendingAction,
                    onPendingAction = onPendingAction,
                    onConfirmed = onResetStreamHealth,
                    modifier = Modifier.width(cardWidth),
                )
                MaintenanceActionCard(
                    title = "清空节目单缓存",
                    description = "当前缓存 ${diagnostics.programCount} 条节目",
                    actionId = MAINTENANCE_CLEAR_EPG,
                    pendingAction = pendingAction,
                    onPendingAction = onPendingAction,
                    onConfirmed = onClearEpgCache,
                    modifier = Modifier.width(cardWidth),
                )
                MaintenanceActionCard(
                    title = "清空观看历史",
                    description = "当前 ${diagnostics.historyCount} 个频道有历史",
                    actionId = MAINTENANCE_CLEAR_HISTORY,
                    pendingAction = pendingAction,
                    onPendingAction = onPendingAction,
                    onConfirmed = onClearWatchHistory,
                    modifier = Modifier.width(cardWidth),
                )
            }
            SettingsHintText("维护操作需要点击两次确认，只影响本地缓存和状态，不删除默认 iptv-org 源。")
        }
    }
}

@Composable
private fun AboutSourcesContent() {
    SettingsCardGrid { cardWidth ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SettingsGridGap)) {
                SettingsValueCard(
                    title = "iptv-org playlist",
                    description = "公开 IPTV 频道索引",
                    value = AppConstants.PRIMARY_PLAYLIST_URL,
                    modifier = Modifier.width(cardWidth),
                )
                SettingsValueCard(
                    title = "EPG / API",
                    description = "节目单与频道元数据来源",
                    value = "https://github.com/iptv-org/epg · https://github.com/iptv-org/api",
                    modifier = Modifier.width(cardWidth),
                )
                SettingsTextCard(
                    title = "来源说明",
                    description = "iptv-org 仓库本身不存储视频文件，只收集公开的直播链接；实际可用性会受频道源、地区和网络影响。",
                    modifier = Modifier.width(cardWidth),
                )
                SettingsTextCard(
                    title = "后续可做",
                    description = "官方源范围选择器可以按国家、语言、分类或来源切换 playlist，但这会影响首页频道范围，建议单独做。",
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

@Composable
private fun SettingsCardGrid(content: @Composable (cardWidth: Dp) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - SettingsGridGap * 3f) / 4f
        content(cardWidth)
    }
}

@Composable
private fun SettingsValueCard(
    title: String,
    description: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        SettingsCardTitle(title = title, description = description)
        Text(
            text = value,
            color = WhaleTokens.PrimaryText,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WhaleTokens.Muted)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SourceStatusCard(
    title: String,
    note: String,
    url: String,
    enabled: Boolean,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier, alpha = if (enabled) 1f else 0.72f) {
        SettingsCardTitle(title = title, description = note)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = url,
                color = if (enabled) WhaleTokens.PrimaryText else WhaleTokens.SecondaryText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WhaleTokens.Muted.copy(alpha = if (enabled) 1f else 0.54f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            SettingsSmallButton(text = actionText, enabled = enabled, onClick = onAction)
        }
    }
}

@Composable
private fun SettingsMetricCard(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        SettingsCardTitle(title = title, description = description)
        Text(
            text = value,
            color = WhaleTokens.Cyan,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsTextCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        Text(title, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            text = description,
            color = WhaleTokens.SecondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MaintenanceActionCard(
    title: String,
    description: String,
    actionId: String,
    pendingAction: String?,
    onPendingAction: (String) -> Unit,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirming = pendingAction == actionId
    SettingsCard(
        modifier = modifier,
        borderColor = if (confirming) WhaleTokens.Gold.copy(alpha = 0.70f) else null,
    ) {
        SettingsCardTitle(
            title = title,
            description = if (confirming) "再次点击确认执行" else description,
        )
        Spacer(Modifier.weight(1f))
        SettingsSmallButton(
            text = if (confirming) "确认" else "执行",
            highlighted = confirming,
            onClick = {
                if (confirming) {
                    onConfirmed()
                } else {
                    onPendingAction(actionId)
                }
            },
        )
    }
}

@Composable
private fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    SettingsCard(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) },
        borderColor = if (focused) WhaleTokens.Cyan.copy(alpha = 0.70f) else null,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = WhaleTokens.SecondaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WhaleTokens.Background,
                    checkedTrackColor = WhaleTokens.Cyan,
                    checkedBorderColor = WhaleTokens.Cyan,
                    uncheckedThumbColor = WhaleTokens.SecondaryText,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.16f),
                ),
            )
        }
    }
}

@Composable
private fun SettingsIntervalCard(
    value: Int,
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier, alpha = if (enabled) 1f else 0.56f) {
        SettingsCardTitle(
            title = "刷新间隔",
            description = "范围 ${SettingsRepository.MIN_REFRESH_INTERVAL_HOURS}-${SettingsRepository.MAX_REFRESH_INTERVAL_HOURS} 小时",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsStepButton(text = "-", enabled = enabled, onClick = { onStep(-1) })
            CompactSettingsInput(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                modifier = Modifier.width(78.dp),
                suffix = "小时",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            SettingsStepButton(text = "+", enabled = enabled, onClick = { onStep(1) })
        }
        Text("当前 $value 小时", color = WhaleTokens.TertiaryText, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SettingsStatusCard(
    label: String,
    description: String,
    lastAttemptAt: Long?,
    lastSuccessAt: Long?,
    error: String?,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val visual = statusVisual(isRefreshing = isRefreshing, lastSuccessAt = lastSuccessAt, error = error)
    SettingsCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(visual.color),
            )
            Text(label, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            StatusPill(text = visual.text, color = visual.color)
        }
        Text(description, color = WhaleTokens.SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = "尝试 ${lastAttemptAt?.let(::formatSyncTime) ?: "尚未"} · 成功 ${lastSuccessAt?.let(::formatSyncTime) ?: "尚未"}",
            color = WhaleTokens.TertiaryText,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        error?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = WhaleTokens.Red, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .height(SettingsCardHeight)
            .clip(shape)
            .background(WhaleTokens.SurfaceRaised.copy(alpha = alpha))
            .border(1.dp, borderColor ?: Color.White.copy(alpha = 0.06f * alpha), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun SettingsCardTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(description, color = WhaleTokens.SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsHintText(text: String) {
    Text(
        text = text,
        color = WhaleTokens.SecondaryText,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp),
    )
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun TopBarIconButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = topBarActionColor(enabled = enabled, highlighted = focused),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsTopBarAction(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = highlighted || focused
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = topBarActionColor(enabled = enabled, highlighted = active),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            color = if (enabled) WhaleTokens.PrimaryText else WhaleTokens.SecondaryText.copy(alpha = 0.38f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SettingsStepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    SettingButtonContainer(onClick = onClick, enabled = enabled, iconOnly = true) {
        Text(text, color = settingsButtonTextColor(enabled), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsSmallButton(
    text: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    SettingButtonContainer(onClick = onClick, enabled = enabled, highlighted = highlighted) {
        Text(text, color = settingsButtonTextColor(enabled), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompactSettingsInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(
            color = if (enabled) WhaleTokens.PrimaryText else WhaleTokens.SecondaryText.copy(alpha = 0.48f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        ),
        cursorBrush = SolidColor(WhaleTokens.Cyan),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(WhaleTokens.Muted.copy(alpha = if (enabled) 1f else 0.56f))
            .border(
                1.dp,
                if (enabled && focused) WhaleTokens.Cyan.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.08f),
                shape,
            )
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isBlank() && placeholder.isNotBlank()) {
                        Text(
                            text = placeholder,
                            color = WhaleTokens.SecondaryText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                suffix?.let {
                    Text(it, color = WhaleTokens.SecondaryText, fontSize = 12.sp, maxLines = 1)
                }
            }
        },
    )
}

@Composable
private fun SettingButtonContainer(
    onClick: () -> Unit,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    iconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .height(36.dp)
            .then(if (iconOnly) Modifier.width(36.dp) else Modifier)
            .clip(shape)
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    highlighted || focused -> WhaleTokens.Cyan.copy(alpha = 0.14f)
                    else -> Color.White.copy(alpha = 0.07f)
                },
            )
            .border(
                1.dp,
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    focused -> WhaleTokens.Cyan.copy(alpha = 0.70f)
                    highlighted -> WhaleTokens.Cyan.copy(alpha = 0.30f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (iconOnly) 0.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}

private fun settingsButtonTextColor(enabled: Boolean): Color {
    return if (enabled) WhaleTokens.PrimaryText else WhaleTokens.SecondaryText.copy(alpha = 0.38f)
}

private fun topBarActionColor(enabled: Boolean, highlighted: Boolean): Color {
    return when {
        !enabled -> WhaleTokens.SecondaryText.copy(alpha = 0.38f)
        highlighted -> WhaleTokens.Cyan
        else -> WhaleTokens.SecondaryText
    }
}

private fun settingsHeaderStatus(
    isRefreshing: Boolean,
    hasUnsavedChanges: Boolean,
    message: String?,
): StatusVisual {
    return when {
        isRefreshing -> StatusVisual("正在刷新", WhaleTokens.Cyan)
        hasUnsavedChanges -> StatusVisual("未保存", WhaleTokens.Gold)
        message?.contains("失败") == true -> StatusVisual("失败", WhaleTokens.Red)
        message?.contains("已刷新") == true || message?.contains("已更新") == true -> StatusVisual("已刷新", WhaleTokens.Green)
        else -> StatusVisual("已保存", WhaleTokens.Green)
    }
}

private fun statusVisual(isRefreshing: Boolean, lastSuccessAt: Long?, error: String?): StatusVisual {
    return when {
        isRefreshing -> StatusVisual("刷新中", WhaleTokens.Cyan)
        !error.isNullOrBlank() -> StatusVisual("失败", WhaleTokens.Red)
        lastSuccessAt != null -> StatusVisual("已同步", WhaleTokens.Green)
        else -> StatusVisual("未同步", WhaleTokens.SecondaryText)
    }
}

private fun formatSyncTime(value: Long): String {
    return SyncTimeFormatter.format(Instant.ofEpochMilli(value))
}

private fun settingsMenuSpecs(): List<SettingsMenuSpec> = listOf(
    SettingsMenuSpec(
        menu = SettingsMenu.Source,
        title = "数据源",
        description = "Playlist",
        longDescription = "查看默认开源频道和节目单来源",
        icon = Icons.Default.Storage,
    ),
    SettingsMenuSpec(
        menu = SettingsMenu.Epg,
        title = "节目单",
        description = "XMLTV / EPG",
        longDescription = "查看当前节目和下一节目的开源数据来源",
        icon = Icons.AutoMirrored.Filled.EventNote,
    ),
    SettingsMenuSpec(
        menu = SettingsMenu.Refresh,
        title = "自动刷新",
        description = "周期同步",
        longDescription = "控制频道和节目单的后台同步频率",
        icon = Icons.Default.Sync,
    ),
    SettingsMenuSpec(
        menu = SettingsMenu.Status,
        title = "同步状态",
        description = "最近结果",
        longDescription = "查看频道源和节目单最近一次同步结果",
        icon = Icons.Default.CheckCircle,
    ),
    SettingsMenuSpec(
        menu = SettingsMenu.Maintenance,
        title = "维护",
        description = "本地缓存",
        longDescription = "重置播放源状态，清理节目单和观看历史",
        icon = Icons.Default.Build,
    ),
    SettingsMenuSpec(
        menu = SettingsMenu.About,
        title = "关于来源",
        description = "开源说明",
        longDescription = "查看 iptv-org、EPG 与 API 来源说明",
        icon = Icons.Default.Info,
    ),
)

private enum class SettingsMenu {
    Source,
    Epg,
    Refresh,
    Status,
    Maintenance,
    About,
}

private data class SettingsMenuSpec(
    val menu: SettingsMenu,
    val title: String,
    val description: String,
    val longDescription: String,
    val icon: ImageVector,
)

private data class StatusVisual(
    val text: String,
    val color: Color,
)

private val SyncTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
private val SettingsGridGap = 20.dp
private val SettingsCardHeight = 124.dp
private const val MAINTENANCE_RESET_STREAMS = "reset_streams"
private const val MAINTENANCE_CLEAR_EPG = "clear_epg"
private const val MAINTENANCE_CLEAR_HISTORY = "clear_history"
