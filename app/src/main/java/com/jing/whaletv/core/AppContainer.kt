package com.jing.whaletv.core

import android.content.Context
import com.jing.whaletv.data.local.WhaleTvDatabase
import com.jing.whaletv.data.network.PlaylistClient
import com.jing.whaletv.data.repository.ChannelRepository
import com.jing.whaletv.data.repository.SettingsRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database: WhaleTvDatabase = WhaleTvDatabase.get(appContext)
    private val playlistClient: PlaylistClient = PlaylistClient()

    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val channelRepository: ChannelRepository = ChannelRepository(
        database = database,
        settingsRepository = settingsRepository,
        playlistClient = playlistClient,
    )
}
