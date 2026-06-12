package com.jing.whaletv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jing.whaletv.core.AppContainer
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.isPlayable
import com.jing.whaletv.data.model.playbackStreams
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
    val syncSummary: SyncSummary = SyncSummary(),
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val playingChannelId: String? = null,
)

private data class UiPartialState(
    val channels: List<TvChannel>,
    val syncSummary: SyncSummary,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val playingChannelId: String? = null,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val playingChannelId = MutableStateFlow<String?>(null)
    private val syncMutex = Mutex()

    private val uiSource: StateFlow<UiPartialState> = combine(
        combine(
            container.channelRepository.observeChannels(),
            container.channelRepository.observeSyncSummary(),
        ) { channels, syncSummary ->
            UiPartialState(channels = channels, syncSummary = syncSummary)
        },
        isRefreshing,
    ) { base, refreshing ->
        base.copy(isRefreshing = refreshing)
    }.combine(message) { base, msg ->
        base.copy(message = msg)
    }.combine(playingChannelId) { base, channelId ->
        base.copy(playingChannelId = channelId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiPartialState(emptyList(), SyncSummary()),
    )

    val uiState: StateFlow<HomeUiState> = uiSource
        .map { state ->
            HomeUiState(
                channels = state.channels.filter { it.isPlayable() },
                syncSummary = state.syncSummary,
                isRefreshing = state.isRefreshing,
                message = state.message,
                playingChannelId = state.playingChannelId,
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
                successMessage = "频道已更新",
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
