package com.jing.whaletv.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.repository.SettingsRepository
import com.jing.whaletv.ui.components.TvFocusStyle
import com.jing.whaletv.ui.components.TvFocusable
import com.jing.whaletv.ui.components.TvTextButton
import com.jing.whaletv.ui.components.WhaleTopBar
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    settings: AppSettings,
    syncSummary: SyncSummary,
    diagnostics: SettingsDiagnostics,
    effectiveEpgUrl: String?,
    isRefreshing: Boolean,
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
    var playlistScopeId by rememberSaveable { mutableStateOf(settings.playlistScope.id) }
    var pendingMaintenanceAction by rememberSaveable { mutableStateOf<String?>(null) }
    val refreshInterval = refreshIntervalText.toIntOrNull()
        ?.let(SettingsRepository::normalizeRefreshIntervalHours)
        ?: AppConstants.DEFAULT_REFRESH_INTERVAL_HOURS
    val draftSettings = AppSettings(
        autoRefresh = autoRefresh,
        refreshIntervalHours = refreshInterval,
        playlistScope = PlaylistScope.fromId(playlistScopeId),
        homeCountryTabIds = settings.homeCountryTabIds,
    )
    val normalizedSettings = settings.copy(
        refreshIntervalHours = SettingsRepository.normalizeRefreshIntervalHours(settings.refreshIntervalHours),
    )
    val sourceHasUnsavedChanges = draftSettings.playlistScope != normalizedSettings.playlistScope
    val refreshHasUnsavedChanges = draftSettings.autoRefresh != normalizedSettings.autoRefresh ||
        draftSettings.refreshIntervalHours != normalizedSettings.refreshIntervalHours
    val hasUnsavedChanges = sourceHasUnsavedChanges || refreshHasUnsavedChanges
    val selectedMenu = SettingsMenu.values().firstOrNull { it.name == selectedMenuId } ?: SettingsMenu.Source
    val menuSpecs = remember { settingsMenuSpecs() }
    val selectedSpec = menuSpecs.first { it.menu == selectedMenu }
    var savedFeedbackMenu by remember { mutableStateOf<SettingsMenu?>(null) }
    var saveFeedbackEvent by remember { mutableStateOf(0) }
    val headerStatus = settingsHeaderStatus(
        selectedMenu = selectedMenu,
        sourceHasUnsavedChanges = sourceHasUnsavedChanges,
        refreshHasUnsavedChanges = refreshHasUnsavedChanges,
        savedFeedbackMenu = savedFeedbackMenu,
    )

    LaunchedEffect(settings) {
        autoRefresh = settings.autoRefresh
        refreshIntervalText = settings.refreshIntervalHours.toString()
        playlistScopeId = settings.playlistScope.id
    }
    LaunchedEffect(selectedMenu) {
        if (savedFeedbackMenu != selectedMenu) {
            savedFeedbackMenu = null
        }
        // 切换分区时清除维护操作残留的二次确认状态
        pendingMaintenanceAction = null
    }
    LaunchedEffect(saveFeedbackEvent, savedFeedbackMenu) {
        if (saveFeedbackEvent > 0 && savedFeedbackMenu != null) {
            delay(SettingsSaveFeedbackDurationMillis)
            savedFeedbackMenu = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleTokens.Background),
    ) {
        SettingsTopBar(
            isRefreshing = isRefreshing,
            onBack = onBack,
            onSave = {
                savedFeedbackMenu = selectedMenu.takeIf { it.supportsSaveFeedback }
                saveFeedbackEvent += 1
                onSave(draftSettings)
            },
            onRefreshNow = onRefreshNow,
            hasUnsavedChanges = hasUnsavedChanges,
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
                headerStatus = headerStatus,
                autoRefresh = autoRefresh,
                onAutoRefreshChange = { autoRefresh = it },
                playlistScope = draftSettings.playlistScope,
                onPlaylistScopeChange = { playlistScopeId = it.id },
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

@Composable
private fun SettingsTopBar(
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onRefreshNow: () -> Unit,
    hasUnsavedChanges: Boolean,
) {
    WhaleTopBar(
        title = "设置",
        onBack = onBack,
        actions = {
            TvTextButton(
                text = "立即刷新",
                icon = Icons.Default.Refresh,
                enabled = !isRefreshing,
                onClick = onRefreshNow,
            )
            Spacer(Modifier.width(12.dp))
            TvTextButton(
                text = "保存",
                icon = Icons.Default.Save,
                emphasized = hasUnsavedChanges,
                onClick = onSave,
            )
        },
    )
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
    TvFocusable(
        onClick = onClick,
        selected = selected,
        shape = WhaleShapes.Card,
        style = TvFocusStyle(
            fillSelected = WhaleTokens.SurfaceRaised,
            borderSelected = Color.Transparent,
        ),
        onFocusChanged = { focused ->
            // 焦点落上即切换菜单，保持原有遥控浏览行为
            if (focused && !selected) {
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) { focused ->
        val active = selected || focused
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(WhaleTokens.Accent),
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
                tint = if (active) WhaleTokens.Accent else WhaleTokens.IconMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                spec.title,
                color = if (active) WhaleTokens.Accent else WhaleTokens.TextPrimary,
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
    headerStatus: StatusVisual?,
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    playlistScope: PlaylistScope,
    onPlaylistScopeChange: (PlaylistScope) -> Unit,
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
            status = headerStatus,
        )
        when (selectedSpec.menu) {
            SettingsMenu.Source -> SourceSettingsContent(
                playlistScope = playlistScope,
                syncSummary = syncSummary,
                onPlaylistScopeChange = onPlaylistScopeChange,
                effectiveEpgUrl = effectiveEpgUrl,
                onTestDefaultPlaylistSource = onTestDefaultPlaylistSource,
                onTestActiveEpgSource = onTestActiveEpgSource,
            )
            SettingsMenu.Epg -> EpgSettingsContent(
                effectiveEpgUrl = effectiveEpgUrl,
                syncSummary = syncSummary,
                diagnostics = diagnostics,
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
private fun ContentHeader(spec: SettingsMenuSpec, status: StatusVisual?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(spec.title, color = WhaleTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = spec.longDescription,
            color = WhaleTokens.TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        status?.let {
            StatusPill(text = it.text, color = it.color)
        }
    }
}

private fun settingsHeaderStatus(
    selectedMenu: SettingsMenu,
    sourceHasUnsavedChanges: Boolean,
    refreshHasUnsavedChanges: Boolean,
    savedFeedbackMenu: SettingsMenu?,
): StatusVisual? {
    val currentPageHasUnsavedChanges = when (selectedMenu) {
        SettingsMenu.Source -> sourceHasUnsavedChanges
        SettingsMenu.Refresh -> refreshHasUnsavedChanges
        else -> false
    }
    return when {
        currentPageHasUnsavedChanges -> StatusVisual("未保存", WhaleTokens.Gold)
        selectedMenu.supportsSaveFeedback && savedFeedbackMenu == selectedMenu -> StatusVisual("已保存", WhaleTokens.Green)
        else -> null
    }
}

private fun settingsMenuSpecs(): List<SettingsMenuSpec> = listOf(
    SettingsMenuSpec(
        menu = SettingsMenu.Source,
        title = "数据源",
        description = "Playlist",
        longDescription = "选择优先更新的官方频道来源",
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
        longDescription = "查看优先更新、后台补全和节目单结果",
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

private val SettingsMenu.supportsSaveFeedback: Boolean
    get() = this == SettingsMenu.Source || this == SettingsMenu.Refresh

private data class SettingsMenuSpec(
    val menu: SettingsMenu,
    val title: String,
    val description: String,
    val longDescription: String,
    val icon: ImageVector,
)

private const val SettingsSaveFeedbackDurationMillis = 5_000L
