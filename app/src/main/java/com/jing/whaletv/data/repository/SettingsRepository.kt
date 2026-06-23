package com.jing.whaletv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.ui.HomeCountryTabs
import com.jing.whaletv.ui.normalizeHomeCountryTabIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.whaleSettingsStore by preferencesDataStore("whale_tv_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.whaleSettingsStore)

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            autoRefresh = prefs[Keys.autoRefresh] ?: true,
            refreshIntervalHours = normalizeRefreshIntervalHours(
                prefs[Keys.refreshIntervalHours] ?: AppConstants.DEFAULT_REFRESH_INTERVAL_HOURS,
            ),
            playlistScope = PlaylistScope.fromId(prefs[Keys.playlistScope]),
            homeCountryTabIds = normalizeStoredHomeCountryTabIds(
                prefs[Keys.homeCountryTabIds]?.split(",").orEmpty(),
            ),
        )
    }

    suspend fun saveSettings(settings: AppSettings): AppSettings {
        val normalized = settings.normalized()
        dataStore.edit { prefs ->
            prefs.remove(Keys.legacyCustomPlaylistUrl)
            prefs.remove(Keys.legacyXmltvUrl)
            prefs[Keys.autoRefresh] = normalized.autoRefresh
            prefs[Keys.refreshIntervalHours] = normalized.refreshIntervalHours
            prefs[Keys.playlistScope] = normalized.playlistScope.id
            prefs[Keys.homeCountryTabIds] = normalized.homeCountryTabIds.joinToString(",")
        }
        return normalized
    }

    private object Keys {
        val legacyCustomPlaylistUrl = stringPreferencesKey("custom_playlist_url")
        val legacyXmltvUrl = stringPreferencesKey("xmltv_url")
        val autoRefresh = booleanPreferencesKey("auto_refresh")
        val refreshIntervalHours = intPreferencesKey("refresh_interval_hours")
        val playlistScope = stringPreferencesKey("playlist_scope")
        val homeCountryTabIds = stringPreferencesKey("home_country_tab_ids")
    }

    companion object {
        const val MIN_REFRESH_INTERVAL_HOURS = 1
        const val MAX_REFRESH_INTERVAL_HOURS = 72

        fun normalizeRefreshIntervalHours(value: Int): Int {
            return value.coerceIn(MIN_REFRESH_INTERVAL_HOURS, MAX_REFRESH_INTERVAL_HOURS)
        }
    }
}

private fun AppSettings.normalized(): AppSettings {
    return copy(
        refreshIntervalHours = SettingsRepository.normalizeRefreshIntervalHours(refreshIntervalHours),
        homeCountryTabIds = normalizeStoredHomeCountryTabIds(homeCountryTabIds),
    )
}

private fun normalizeStoredHomeCountryTabIds(ids: List<String>): List<String> {
    val validIds = ids
        .map { it.trim().lowercase() }
        .filter { it == "other" || countryIdPattern.matches(it) }
    if (validIds.isEmpty()) return HomeCountryTabs.map { it.id }
    return normalizeHomeCountryTabIds(validIds)
}

private val countryIdPattern = Regex("[a-z]{2}")
