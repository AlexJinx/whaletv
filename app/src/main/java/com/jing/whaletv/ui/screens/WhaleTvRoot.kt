package com.jing.whaletv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jing.whaletv.ui.MainViewModel
import com.jing.whaletv.data.model.isPlayable

@Composable
fun WhaleTvRoot(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf("home") }
    var selectedChannelId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailBackRoute by rememberSaveable { mutableStateOf("home") }
    var autoOpened by remember { mutableStateOf(false) }
    val selectedChannel = state.channels.firstOrNull { it.id == selectedChannelId }

    LaunchedEffect(state.channels, state.settings.openLastChannel, state.settings.lastChannelId) {
        if (!autoOpened && state.settings.openLastChannel && state.settings.lastChannelId != null) {
            val channel = state.channels.firstOrNull { it.id == state.settings.lastChannelId }
            if (channel != null) {
                autoOpened = true
                selectedChannelId = channel.id
                route = "player"
            }
        }
    }

    when (route) {
        "player" -> {
            if (selectedChannel == null) {
                route = "home"
            } else if (!selectedChannel.isPlayable()) {
                viewModel.markChannelUnavailable(selectedChannel.id)
                selectedChannelId = null
                route = "home"
            } else {
                PlayerScreen(
                    channel = selectedChannel,
                    channels = state.channels,
                    onBack = { route = "home" },
                    onOpenChannel = {
                        if (it.isPlayable()) {
                            selectedChannelId = it.id
                        } else {
                            viewModel.markChannelUnavailable(it.id)
                        }
                    },
                    onOpenDetail = {
                        if (it.isPlayable()) {
                            selectedChannelId = it.id
                            detailBackRoute = "player"
                            route = "epg"
                        } else {
                            viewModel.markChannelUnavailable(it.id)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onStreamHealthy = viewModel::markStreamHealthy,
                    onStreamFailed = viewModel::markStreamFailed,
                    onStreamTimeout = viewModel::markStreamTimeoutFailed,
                    onNoStreamAvailable = { channelId ->
                        viewModel.markChannelUnavailable(channelId)
                        selectedChannelId = null
                        route = "home"
                    },
                    onWatched = viewModel::markWatched,
                )
            }
        }
        "epg" -> {
            if (selectedChannel == null) {
                route = "home"
            } else {
                EPGScreen(
                    channel = selectedChannel,
                    onBack = { route = detailBackRoute },
                    onPlay = {
                        if (it.isPlayable()) {
                            selectedChannelId = it.id
                            route = "player"
                        } else {
                            viewModel.markChannelUnavailable(it.id)
                        }
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                )
            }
        }
        "search" -> SearchScreen(
            channels = state.channels,
            onBack = { route = "home" },
            onOpenChannel = {
                if (it.isPlayable()) {
                    selectedChannelId = it.id
                    route = "player"
                } else {
                    viewModel.markChannelUnavailable(it.id)
                }
            },
        )
        "settings" -> SettingsScreen(
            state = state,
            onBack = { route = "home" },
            onCustomPlaylistChanged = viewModel::setCustomPlaylistUrl,
            onXmltvChanged = viewModel::setXmltvUrl,
            onAutoRefreshChanged = viewModel::setAutoRefresh,
            onRefreshIntervalChanged = viewModel::setRefreshIntervalHours,
            onChannelSortModeChanged = viewModel::setChannelSortMode,
            onSectionOrderChanged = viewModel::setVisibleSectionIds,
            onOpenLastChannelChanged = viewModel::setOpenLastChannel,
            onClearCache = viewModel::clearCache,
            onRefreshNow = viewModel::refreshNow,
        )
        else -> HomeScreen(
            state = state,
            onOpenChannel = {
                if (it.isPlayable()) {
                    selectedChannelId = it.id
                    route = "player"
                } else {
                    viewModel.markChannelUnavailable(it.id)
                }
            },
            onOpenDetail = {
                if (it.isPlayable()) {
                    selectedChannelId = it.id
                    detailBackRoute = "home"
                    route = "epg"
                } else {
                    viewModel.markChannelUnavailable(it.id)
                }
            },
            onSearch = { route = "search" },
            onSettings = { route = "settings" },
            onRefresh = viewModel::refreshNow,
        )
    }
}
