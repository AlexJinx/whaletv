package com.jing.whaletv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jing.whaletv.core.AppContainer
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.ChannelSortMode
import com.jing.whaletv.data.model.DEFAULT_VISIBLE_SECTION_IDS
import com.jing.whaletv.data.model.ChannelSection
import com.jing.whaletv.data.model.isPlayable
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.TvStream
import com.jing.whaletv.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex

data class HomeUiState(
    val channels: List<TvChannel> = emptyList(),
    val sections: List<ChannelSection> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val syncSummary: SyncSummary = SyncSummary(),
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

private data class UiPartialState(
    val channels: List<TvChannel>,
    val settings: AppSettings,
    val syncSummary: SyncSummary,
    val suppressed: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val suppressedChannelIds = MutableStateFlow<Set<String>>(emptySet())
    private val syncMutex = Mutex()

    private val uiSource: StateFlow<UiPartialState> = combine(
        combine(
            combine(
                container.channelRepository.observeChannels(),
                container.settingsRepository.settings,
            ) { channels, settings ->
                UiPartialState(channels, settings, SyncSummary())
            },
            container.channelRepository.observeSyncSummary(),
        ) { base, syncSummary ->
            base.copy(syncSummary = syncSummary)
        },
        suppressedChannelIds,
    ) { base, suppressed ->
        base.copy(suppressed = suppressed)
    }.combine(isRefreshing) { base, refreshing ->
        base.copy(isRefreshing = refreshing)
    }.combine(message) { base, msg ->
        base.copy(message = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiPartialState(emptyList(), AppSettings(), SyncSummary(), emptySet(), false, null),
    )

    val uiState: StateFlow<HomeUiState> = uiSource
        .map { state ->
            val visibleChannels = state.channels
                .filter { it.isPlayable() && it.id !in state.suppressed }
            HomeUiState(
                channels = visibleChannels,
                sections = buildSections(
                    channels = visibleChannels,
                    channelSortMode = state.settings.channelSortMode,
                    visibleSectionIds = state.settings.visibleSectionIds,
                ),
                settings = state.settings,
                syncSummary = state.syncSummary,
                isRefreshing = state.isRefreshing,
                message = state.message,
            )
        }
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
                successMessage = "频道已更新",
                failurePrefix = "刷新失败",
            )
        }
    }

    fun toggleFavorite(channel: TvChannel) {
        viewModelScope.launch {
            container.channelRepository.setFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun markWatched(channel: TvChannel) {
        viewModelScope.launch { container.channelRepository.markWatched(channel.id) }
    }

    fun markStreamFailed(stream: TvStream) {
        viewModelScope.launch {
            markStreamFailure(stream, isTimeout = false)
        }
    }

    fun markStreamTimeoutFailed(stream: TvStream) {
        markStreamTimeoutFailed(stream, noPlayableCandidate = false)
    }

    fun markStreamTimeoutFailed(stream: TvStream, noPlayableCandidate: Boolean) {
        viewModelScope.launch {
            markStreamFailure(stream, isTimeout = true, noPlayableCandidate = noPlayableCandidate)
        }
    }

    private suspend fun markStreamFailure(
        stream: TvStream,
        isTimeout: Boolean,
        noPlayableCandidate: Boolean = false,
    ) {
        val hasPlayableStream = container.channelRepository.markStreamFailed(stream, isTimeout = isTimeout)
        if (!hasPlayableStream || (isTimeout && noPlayableCandidate)) {
            suppressedChannelIds.update { it + stream.channelId }
            container.channelRepository.markChannelUnavailable(stream.channelId)
        }
    }

    fun markStreamHealthy(stream: TvStream) {
        viewModelScope.launch {
            suppressedChannelIds.update { it - stream.channelId }
            container.channelRepository.markStreamSucceeded(stream)
        }
    }

    fun markChannelUnavailable(channelId: String) {
        viewModelScope.launch {
            suppressedChannelIds.update { it + channelId }
            container.channelRepository.markChannelUnavailable(channelId)
        }
    }

    fun setCustomPlaylistUrl(value: String) {
        viewModelScope.launch { container.settingsRepository.setCustomPlaylistUrl(value) }
    }

    fun setXmltvUrl(value: String) {
        viewModelScope.launch { container.settingsRepository.setXmltvUrl(value) }
    }

    fun setAutoRefresh(value: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setAutoRefresh(value)
            if (value) {
                SyncScheduler.schedulePeriodic(
                    context = container.appContext,
                    intervalHours = uiState.value.settings.refreshIntervalHours,
                )
            } else {
                SyncScheduler.cancelPeriodic(container.appContext)
            }
        }
    }

    fun setRefreshIntervalHours(value: Int) {
        viewModelScope.launch {
            container.settingsRepository.setRefreshIntervalHours(value)
            if (uiState.value.settings.autoRefresh) {
                SyncScheduler.schedulePeriodic(container.appContext, value)
            }
        }
    }

    fun setHideUnavailable(value: Boolean) {
        viewModelScope.launch { container.settingsRepository.setHideUnavailable(value) }
    }

    fun setOpenLastChannel(value: Boolean) {
        viewModelScope.launch { container.settingsRepository.setOpenLastChannel(value) }
    }

    fun setChannelSortMode(mode: ChannelSortMode) {
        viewModelScope.launch { container.settingsRepository.setChannelSortMode(mode) }
    }

    fun setVisibleSectionIds(sectionIds: List<String>) {
        viewModelScope.launch { container.settingsRepository.setVisibleSectionIds(sectionIds) }
    }

    fun clearCache() {
        viewModelScope.launch {
            suppressedChannelIds.value = emptySet()
            container.channelRepository.clearCache()
            message.value = "本地缓存已清理"
        }
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
                suppressedChannelIds.value = emptySet()
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

    private fun buildSections(
        channels: List<TvChannel>,
        channelSortMode: ChannelSortMode,
        visibleSectionIds: List<String>,
    ): List<ChannelSection> {
        val visibleSections = normalizeSectionIds(visibleSectionIds)
        val sectionMap = TvNavSections.associateBy { it.id }
        return visibleSections.mapNotNull { sectionId ->
            val section = sectionMap[sectionId] ?: return@mapNotNull null
            val sectionChannels = channelsForSection(section.id, channels, channelSortMode)
            if (sectionChannels.isEmpty()) null else ChannelSection(section.id, section.label, sectionChannels)
        }
    }

    private fun normalizeSectionIds(sectionIds: List<String>): List<String> {
        val normalized = sectionIds
            .mapNotNull { id ->
                val trimmed = id.trim()
                if (trimmed in DEFAULT_VISIBLE_SECTION_IDS.toSet()) trimmed else null
            }
            .distinct()
        return if (normalized.isEmpty()) DEFAULT_VISIBLE_SECTION_IDS.toList() else normalized
    }

    private companion object {
        const val STARTUP_STREAM_PRECHECK_WAIT_MS = 8_000L
    }
}

private fun Throwable.userFacingMessage(): String {
    return message
        ?.takeIf { it.isNotBlank() }
        ?: this::class.java.simpleName
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
