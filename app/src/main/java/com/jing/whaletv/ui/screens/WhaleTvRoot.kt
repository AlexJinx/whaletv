package com.jing.whaletv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jing.whaletv.ui.MainViewModel
import com.jing.whaletv.ui.Route
import com.jing.whaletv.ui.screens.settings.SettingsScreen

@Composable
fun WhaleTvRoot(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playingChannel = state.playingChannelId?.let { channelId ->
        state.channels.firstOrNull { it.id == channelId }
    }
    val playerChannel = playingChannel?.copy(
        schedulePrograms = state.playingSchedulePrograms.ifEmpty { playingChannel.schedulePrograms },
    )

    LaunchedEffect(state.playingChannelId, playerChannel) {
        if (state.playingChannelId != null && playerChannel == null) {
            viewModel.closePlayer()
        }
    }

    val homeScreen: @Composable () -> Unit = {
        HomeScreen(
            state = state,
            onRefresh = viewModel::refreshNow,
            onChannelSelected = viewModel::playChannel,
            onSearch = viewModel::openSearch,
            onSettings = viewModel::openSettings,
            onEditCountries = viewModel::openCountryEditor,
        )
    }

    // TV 布局在 Density(1f) 下调优（dp 即物理像素），统一在根部提供一次。
    val platformDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale),
    ) {
        when (state.route) {
            is Route.Player -> {
                if (playerChannel != null) {
                    PlayerScreen(
                        channel = playerChannel,
                        onClose = viewModel::closePlayer,
                        onToggleFavorite = { favorite ->
                            viewModel.toggleFavorite(playerChannel.id, favorite)
                        },
                        onPlaybackReady = { streamUrl ->
                            viewModel.markPlaybackReady(playerChannel.id, streamUrl)
                        },
                        onPlaybackFailed = { streamUrl ->
                            viewModel.markPlaybackFailed(playerChannel.id, streamUrl)
                        },
                    )
                } else {
                    homeScreen()
                }
            }
            Route.Settings -> SettingsScreen(
                settings = state.settings,
                syncSummary = state.syncSummary,
                diagnostics = state.settingsDiagnostics,
                effectiveEpgUrl = state.effectiveEpgUrl,
                isRefreshing = state.isRefreshing,
                onBack = viewModel::closeSettings,
                onSave = viewModel::saveSettings,
                onRefreshNow = viewModel::refreshSettingsNow,
                onTestDefaultPlaylistSource = viewModel::testDefaultPlaylistSource,
                onTestActiveEpgSource = viewModel::testActiveEpgSource,
                onResetStreamHealth = viewModel::resetStreamHealth,
                onClearEpgCache = viewModel::clearEpgCache,
                onClearWatchHistory = viewModel::clearWatchHistory,
            )
            Route.Search -> SearchScreen(
                channels = state.channels,
                onBack = viewModel::closeSearch,
                onChannelSelected = viewModel::playChannel,
            )
            Route.CountryEditor -> CountryEditorScreen(
                channels = state.channels,
                visibleCountryIds = state.countryTabs.map { it.id },
                syncSummary = state.syncSummary,
                isRefreshing = state.isRefreshing,
                message = state.message,
                onBack = viewModel::closeCountryEditor,
                onSave = viewModel::saveCountryTabs,
            )
            Route.Home -> homeScreen()
        }
    }
}
