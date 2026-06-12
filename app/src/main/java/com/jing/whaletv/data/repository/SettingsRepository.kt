package com.jing.whaletv.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.whaleSettingsStore by preferencesDataStore("whale_tv_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.whaleSettingsStore.data.map { prefs ->
        AppSettings(
            customPlaylistUrl = prefs[Keys.customPlaylistUrl].orEmpty(),
            xmltvUrl = prefs[Keys.xmltvUrl].orEmpty(),
            autoRefresh = prefs[Keys.autoRefresh] ?: true,
            refreshIntervalHours = prefs[Keys.refreshIntervalHours] ?: AppConstants.DEFAULT_REFRESH_INTERVAL_HOURS,
        )
    }

    private object Keys {
        val customPlaylistUrl = stringPreferencesKey("custom_playlist_url")
        val xmltvUrl = stringPreferencesKey("xmltv_url")
        val autoRefresh = booleanPreferencesKey("auto_refresh")
        val refreshIntervalHours = intPreferencesKey("refresh_interval_hours")
    }
}
