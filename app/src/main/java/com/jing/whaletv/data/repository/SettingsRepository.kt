package com.jing.whaletv.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    suspend fun setCustomPlaylistUrl(value: String) = editString(Keys.customPlaylistUrl, value)
    suspend fun setXmltvUrl(value: String) = editString(Keys.xmltvUrl, value)
    suspend fun setAutoRefresh(value: Boolean) = editBoolean(Keys.autoRefresh, value)
    suspend fun setRefreshIntervalHours(value: Int) = editInt(Keys.refreshIntervalHours, value.coerceIn(1, 72))

    private suspend fun editString(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        context.whaleSettingsStore.edit { prefs ->
            if (value.isBlank()) prefs.remove(key) else prefs[key] = value.trim()
        }
    }

    private suspend fun editBoolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.whaleSettingsStore.edit { prefs -> prefs[key] = value }
    }

    private suspend fun editInt(key: androidx.datastore.preferences.core.Preferences.Key<Int>, value: Int) {
        context.whaleSettingsStore.edit { prefs -> prefs[key] = value }
    }

    private object Keys {
        val customPlaylistUrl = stringPreferencesKey("custom_playlist_url")
        val xmltvUrl = stringPreferencesKey("xmltv_url")
        val autoRefresh = booleanPreferencesKey("auto_refresh")
        val refreshIntervalHours = intPreferencesKey("refresh_interval_hours")
    }
}
