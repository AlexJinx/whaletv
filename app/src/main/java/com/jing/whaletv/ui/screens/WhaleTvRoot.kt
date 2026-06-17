package com.jing.whaletv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jing.whaletv.ui.MainViewModel

@Composable
fun WhaleTvRoot(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playingChannel = state.playingChannelId?.let { channelId ->
        state.channels.firstOrNull { it.id == channelId }
    }

    LaunchedEffect(state.playingChannelId, playingChannel) {
        if (state.playingChannelId != null && playingChannel == null) {
            viewModel.closePlayer()
        }
    }

    if (playingChannel != null) {
        PlayerScreen(
            channel = playingChannel,
            onClose = viewModel::closePlayer,
            onToggleFavorite = { favorite -> viewModel.toggleFavorite(playingChannel.id, favorite) },
            onPlaybackReady = { streamUrl -> viewModel.markPlaybackReady(playingChannel.id, streamUrl) },
            onPlaybackFailed = { streamUrl -> viewModel.markPlaybackFailed(playingChannel.id, streamUrl) },
        )
    } else if (state.isSettingsOpen) {
        SettingsScreen(
            settings = state.settings,
            syncSummary = state.syncSummary,
            diagnostics = state.settingsDiagnostics,
            effectiveEpgUrl = state.effectiveEpgUrl,
            isRefreshing = state.isRefreshing,
            message = state.message,
            onBack = viewModel::closeSettings,
            onSave = viewModel::saveSettings,
            onRefreshNow = viewModel::refreshSettingsNow,
            onTestDefaultPlaylistSource = viewModel::testDefaultPlaylistSource,
            onTestActiveEpgSource = viewModel::testActiveEpgSource,
            onResetStreamHealth = viewModel::resetStreamHealth,
            onClearEpgCache = viewModel::clearEpgCache,
            onClearWatchHistory = viewModel::clearWatchHistory,
        )
    } else if (state.isSearchOpen) {
        SearchScreen(
            channels = state.channels,
            onBack = viewModel::closeSearch,
            onChannelSelected = viewModel::playChannel,
        )
    } else {
        HomeScreen(
            state = state,
            onRefresh = viewModel::refreshNow,
            onChannelSelected = viewModel::playChannel,
            onSearch = viewModel::openSearch,
            onSettings = viewModel::openSettings,
            onUnavailableFeature = viewModel::showUnavailableFeature,
        )
    }
}
