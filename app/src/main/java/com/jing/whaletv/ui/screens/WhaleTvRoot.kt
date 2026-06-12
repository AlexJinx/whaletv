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
    } else {
        HomeScreen(
            state = state,
            onRefresh = viewModel::refreshNow,
            onChannelSelected = viewModel::openChannel,
            onUnavailableFeature = viewModel::showUnavailableFeature,
        )
    }
}
