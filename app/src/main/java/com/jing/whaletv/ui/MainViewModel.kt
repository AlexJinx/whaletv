package com.jing.whaletv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jing.whaletv.core.AppContainer
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.isPlayable
import com.jing.whaletv.data.model.playbackStreams
import com.jing.whaletv.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

data class HomeUiState(
    val channels: List<TvChannel> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val syncSummary: SyncSummary = SyncSummary(),
    val settingsDiagnostics: SettingsDiagnostics = SettingsDiagnostics(),
    val effectiveEpgUrl: String? = null,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val playingChannelId: String? = null,
    val isSettingsOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
)

private data class UiPartialState(
    val channels: List<TvChannel>,
    val settings: AppSettings,
    val syncSummary: SyncSummary,
    val settingsDiagnostics: SettingsDiagnostics,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val playingChannelId: String? = null,
    val isSettingsOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val playingChannelId = MutableStateFlow<String?>(null)
    private val isSettingsOpen = MutableStateFlow(false)
    private val isSearchOpen = MutableStateFlow(false)
    private val syncMutex = Mutex()

    private val uiSource: StateFlow<UiPartialState> = combine(
        combine(
            container.channelRepository.observeChannels(),
            container.channelRepository.observeSyncSummary(),
            container.settingsRepository.settings,
            container.channelRepository.observeSettingsDiagnostics(),
        ) { channels, syncSummary, settings, diagnostics ->
            UiPartialState(
                channels = channels,
                syncSummary = syncSummary,
                settings = settings,
                settingsDiagnostics = diagnostics,
            )
        },
        isRefreshing,
    ) { base, refreshing ->
        base.copy(isRefreshing = refreshing)
    }.combine(message) { base, msg ->
        base.copy(message = msg)
    }.combine(playingChannelId) { base, channelId ->
        base.copy(playingChannelId = channelId)
    }.combine(isSettingsOpen) { base, settingsOpen ->
        base.copy(isSettingsOpen = settingsOpen)
    }.combine(isSearchOpen) { base, searchOpen ->
        base.copy(isSearchOpen = searchOpen)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiPartialState(
            channels = emptyList(),
            settings = AppSettings(),
            syncSummary = SyncSummary(),
            settingsDiagnostics = SettingsDiagnostics(),
        ),
    )

    val uiState: StateFlow<HomeUiState> = uiSource
        .map { state ->
            HomeUiState(
                channels = state.channels.filter { it.isPlayable() },
                settings = state.settings,
                syncSummary = state.syncSummary,
                settingsDiagnostics = state.settingsDiagnostics,
                effectiveEpgUrl = effectiveEpgUrl(state.syncSummary),
                isRefreshing = state.isRefreshing,
                message = state.message,
                playingChannelId = state.playingChannelId,
                isSettingsOpen = state.isSettingsOpen,
                isSearchOpen = state.isSearchOpen,
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    init {
        runStartupStreamPrecheck()
        syncIfCacheEmpty()
    }

    fun refreshNow() {
        viewModelScope.launch {
            runSync(
                startMessage = "正在刷新频道",
                successMessage = "频道与节目单已更新",
                failurePrefix = "刷新失败",
            )
        }
    }

    fun openChannel(channelId: String) {
        val channel = uiState.value.channels.firstOrNull { it.id == channelId }
        if (channel == null || channel.playbackStreams().isEmpty()) {
            showMessage("频道暂无可播放源")
            return
        }
        playingChannelId.value = channelId
    }

    fun closePlayer() {
        playingChannelId.value = null
    }

    fun openSettings() {
        isSearchOpen.value = false
        isSettingsOpen.value = true
    }

    fun closeSettings() {
        isSettingsOpen.value = false
    }

    fun openSearch() {
        isSettingsOpen.value = false
        isSearchOpen.value = true
    }

    fun closeSearch() {
        isSearchOpen.value = false
    }

    fun refreshSettingsNow() {
        viewModelScope.launch {
            runSync(
                startMessage = "正在刷新数据源",
                successMessage = "数据源已刷新",
                failurePrefix = "刷新失败",
            )
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            val saved = container.settingsRepository.saveSettings(settings)
            if (saved.autoRefresh) {
                SyncScheduler.schedulePeriodic(container.appContext, saved.refreshIntervalHours)
            } else {
                SyncScheduler.cancelPeriodic(container.appContext)
            }
            showMessage("设置已保存")
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

    fun showUnavailableFeature(name: String) {
        showMessage("$name 功能开发中")
    }

    private fun syncIfCacheEmpty() {
        viewModelScope.launch {
            if (container.channelRepository.hasCachedPlayableChannels()) return@launch
            runSync(
                startMessage = "正在同步频道",
                successMessage = "频道已同步",
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
                message.value = successMessage
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
