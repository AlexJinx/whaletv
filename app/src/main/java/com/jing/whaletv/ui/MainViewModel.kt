package com.jing.whaletv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jing.whaletv.core.AppContainer
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.playbackStreams
import com.jing.whaletv.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 屏幕导航路由。播放器优先于所有覆盖层；
 * 覆盖层（设置/搜索/国家编辑）之间互斥（结构上只存一个值）。
 */
sealed interface Route {
    data object Home : Route
    data class Player(val channelId: String) : Route
    data object Settings : Route
    data object Search : Route
    data object CountryEditor : Route
}

data class HomeUiState(
    val channels: List<TvChannel> = emptyList(),
    val countryTabs: List<HomeCountryTabSpec> = HomeCountryTabs,
    val settings: AppSettings = AppSettings(),
    val syncSummary: SyncSummary = SyncSummary(),
    val settingsDiagnostics: SettingsDiagnostics = SettingsDiagnostics(),
    val effectiveEpgUrl: String? = null,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val playingChannelId: String? = null,
    val playingSchedulePrograms: List<Program> = emptyList(),
    val route: Route = Route.Home,
)

private data class TransientUiState(
    val isRefreshing: Boolean,
    val message: String?,
    val playingChannelId: String?,
    val playingSchedulePrograms: List<Program>,
    val overlayRoute: Route,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val playingChannelId = MutableStateFlow<String?>(null)
    private val playingSchedulePrograms = MutableStateFlow<List<Program>>(emptyList())
    private val overlayRoute = MutableStateFlow<Route>(Route.Home)
    private val syncMutex = Mutex()

    private val transientState = combine(
        isRefreshing,
        message,
        playingChannelId,
        playingSchedulePrograms,
        overlayRoute,
    ) { refreshing, msg, playingId, schedulePrograms, overlay ->
        TransientUiState(
            isRefreshing = refreshing,
            message = msg,
            playingChannelId = playingId,
            playingSchedulePrograms = schedulePrograms,
            overlayRoute = overlay,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        container.channelRepository.observeChannels(),
        container.channelRepository.observeSyncSummary(),
        container.settingsRepository.settings,
        container.channelRepository.observeSettingsDiagnostics(),
        transientState,
    ) { channels, syncSummary, settings, diagnostics, transient ->
        HomeUiState(
            channels = channels,
            countryTabs = homeCountryTabsForIds(settings.homeCountryTabIds, channels),
            settings = settings,
            syncSummary = syncSummary,
            settingsDiagnostics = diagnostics,
            effectiveEpgUrl = settings.customXmltvUrl ?: effectiveEpgUrl(syncSummary),
            isRefreshing = transient.isRefreshing,
            message = transient.message,
            playingChannelId = transient.playingChannelId,
            playingSchedulePrograms = transient.playingSchedulePrograms,
            route = transient.playingChannelId
                ?.let { Route.Player(it) }
                ?: transient.overlayRoute,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    init {
        observePlayingSchedule()
        runStartupStreamPrecheck()
        syncIfCacheEmpty()
    }

    fun refreshNow() {
        viewModelScope.launch {
            runSync(
                startMessage = "正在优先更新频道",
                successMessage = "优先频道和节目单已更新",
                failurePrefix = "刷新失败",
            )
        }
    }

    fun playChannel(channelId: String) {
        val channel = uiState.value.channels.firstOrNull { it.id == channelId }
        val playbackStreams = channel?.playbackStreams().orEmpty()
        if (channel == null || playbackStreams.isEmpty()) {
            showMessage("频道暂无可播放源")
            return
        }
        playingChannelId.value = channelId
    }

    fun closePlayer() {
        playingChannelId.value = null
    }

    private fun observePlayingSchedule() {
        viewModelScope.launch {
            playingChannelId.collectLatest { channelId ->
                if (channelId == null) {
                    playingSchedulePrograms.value = emptyList()
                    return@collectLatest
                }
                container.channelRepository.observeProgramsForChannel(channelId).collect { programs ->
                    playingSchedulePrograms.value = programs
                }
            }
        }
    }

    fun openSettings() {
        overlayRoute.value = Route.Settings
    }

    fun closeSettings() {
        if (overlayRoute.value == Route.Settings) {
            overlayRoute.value = Route.Home
        }
    }

    fun openSearch() {
        overlayRoute.value = Route.Search
    }

    fun closeSearch() {
        if (overlayRoute.value == Route.Search) {
            overlayRoute.value = Route.Home
        }
    }

    fun openCountryEditor() {
        overlayRoute.value = Route.CountryEditor
    }

    fun closeCountryEditor() {
        if (overlayRoute.value == Route.CountryEditor) {
            overlayRoute.value = Route.Home
        }
    }

    fun saveCountryTabs(countryIds: List<String>) {
        viewModelScope.launch {
            val saved = container.settingsRepository.saveSettings(
                uiState.value.settings.copy(homeCountryTabIds = normalizeHomeCountryTabIds(countryIds)),
            )
            closeCountryEditor()
            showMessage(
                if (saved.homeCountryTabIds == HomeCountryTabs.map { it.id }) {
                    "国家入口已恢复默认"
                } else {
                    "国家入口已保存"
                },
            )
        }
    }

    fun refreshSettingsNow() {
        viewModelScope.launch {
            runSync(
                startMessage = "正在优先更新数据源",
                successMessage = "优先数据源已更新",
                failurePrefix = "刷新失败",
            )
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            val previous = uiState.value.settings
            val saved = container.settingsRepository.saveSettings(settings)
            if (saved.autoRefresh) {
                SyncScheduler.schedulePeriodic(container.appContext, saved.refreshIntervalHours)
            } else {
                SyncScheduler.cancelPeriodic(container.appContext)
            }
            if (saved.playlistScope != previous.playlistScope) {
                runSync(
                    startMessage = "正在优先更新${saved.playlistScope.label}",
                    successMessage = "优先更新范围已更新",
                    failurePrefix = "切换失败",
                )
            } else {
                showMessage("设置已保存")
            }
        }
    }

    fun testDefaultPlaylistSource() {
        viewModelScope.launch {
            val result = container.channelRepository.testDefaultPlaylistSource()
            showMessage(result.message)
        }
    }

    fun testActiveEpgSource() {
        viewModelScope.launch {
            val result = container.channelRepository.testActiveEpgSource()
            showMessage(result.message)
        }
    }

    fun resetStreamHealth() {
        viewModelScope.launch {
            container.channelRepository.resetStreamHealth()
            showMessage("播放源健康状态已重置")
        }
    }

    fun clearEpgCache() {
        viewModelScope.launch {
            container.channelRepository.clearEpgCache()
            showMessage("节目单缓存已清空")
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            container.channelRepository.clearWatchHistory()
            showMessage("观看历史已清空")
        }
    }

    fun toggleFavorite(channelId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            container.channelRepository.setChannelFavorite(channelId, isFavorite)
            showMessage(if (isFavorite) "已加入收藏" else "已取消收藏")
        }
    }

    fun markPlaybackReady(channelId: String, streamUrl: String) {
        viewModelScope.launch {
            container.channelRepository.markPlaybackReady(channelId, streamUrl)
        }
    }

    fun markPlaybackFailed(channelId: String, streamUrl: String) {
        viewModelScope.launch {
            container.channelRepository.markPlaybackFailed(channelId, streamUrl)
        }
    }

    private fun syncIfCacheEmpty() {
        viewModelScope.launch {
            if (container.channelRepository.hasCachedPlayableChannels()) return@launch
            runSync(
                startMessage = "正在优先更新频道",
                successMessage = "优先频道已更新",
                failurePrefix = "同步失败",
            )
        }
    }

    private fun runStartupStreamPrecheck() {
        viewModelScope.launch {
            val hasInitialChannels = withTimeoutOrNull(STARTUP_STREAM_PRECHECK_WAIT_MS) {
                container.channelRepository.observeChannels().first { it.isNotEmpty() }
            }
            if (hasInitialChannels == null) return@launch

            container.channelRepository.precheckStartupStreams()
        }
    }

    private suspend fun runSync(startMessage: String, successMessage: String, failurePrefix: String) {
        if (!syncMutex.tryLock()) {
            message.value = "正在同步频道"
            return
        }
        try {
            isRefreshing.value = true
            message.value = startMessage
            try {
                container.channelRepository.syncPlaylists()
                container.channelRepository.syncEpg()
                if (container.channelRepository.shouldBackfillAllPlaylists()) {
                    SyncScheduler.enqueueFullBackfill(container.appContext)
                    message.value = "$successMessage，后台补全全部频道中"
                } else {
                    message.value = successMessage
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                message.value = "$failurePrefix：${error.userFacingMessage()}"
            }
        } finally {
            isRefreshing.value = false
            syncMutex.unlock()
        }
    }

    private fun showMessage(text: String) {
        viewModelScope.launch {
            message.value = text
            delay(MESSAGE_TIMEOUT_MS)
            if (message.value == text) {
                message.value = null
            }
        }
    }

    private companion object {
        const val STARTUP_STREAM_PRECHECK_WAIT_MS = 8_000L
        const val MESSAGE_TIMEOUT_MS = 3_000L
    }
}

private fun Throwable.userFacingMessage(): String {
    return message
        ?.takeIf { it.isNotBlank() }
        ?: this::class.java.simpleName
}

internal fun effectiveEpgUrl(syncSummary: SyncSummary): String? {
    return syncSummary.discoveredEpgUrl
        ?.takeIf { it.isNotBlank() }
}

class MainViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(container) as T
        }
        error("Unknown ViewModel class $modelClass")
    }
}
