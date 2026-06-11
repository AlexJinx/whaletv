package com.jing.whaletv.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jing.whaletv.core.AppConstants
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.DEFAULT_VISIBLE_SECTION_IDS
import com.jing.whaletv.data.model.ChannelSortMode
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
            hideUnavailable = prefs[Keys.hideUnavailable] ?: false,
            channelSortMode = ChannelSortMode.entries.firstOrNull { it.name == prefs[Keys.channelSortMode] } ?: ChannelSortMode.Default,
            visibleSectionIds = normalizeSectionIds(parseSectionIds(prefs[Keys.visibleSectionIds])),
            openLastChannel = prefs[Keys.openLastChannel] ?: true,
            lastChannelId = prefs[Keys.lastChannelId],
        )
    }

    suspend fun setCustomPlaylistUrl(value: String) = editString(Keys.customPlaylistUrl, value)
    suspend fun setXmltvUrl(value: String) = editString(Keys.xmltvUrl, value)
    suspend fun setAutoRefresh(value: Boolean) = editBoolean(Keys.autoRefresh, value)
    suspend fun setRefreshIntervalHours(value: Int) = editInt(Keys.refreshIntervalHours, value.coerceIn(1, 72))
    suspend fun setHideUnavailable(value: Boolean) = editBoolean(Keys.hideUnavailable, value)
    suspend fun setOpenLastChannel(value: Boolean) = editBoolean(Keys.openLastChannel, value)
    suspend fun setChannelSortMode(mode: ChannelSortMode) = editString(Keys.channelSortMode, mode.name)
    suspend fun setVisibleSectionIds(value: List<String>) = editString(Keys.visibleSectionIds, encodeSectionIds(value))
    suspend fun setLastChannelId(value: String?) = editString(Keys.lastChannelId, value.orEmpty())

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
        val hideUnavailable = booleanPreferencesKey("hide_unavailable")
        val channelSortMode = stringPreferencesKey("channel_sort_mode")
        val visibleSectionIds = stringPreferencesKey("visible_section_ids")
        val openLastChannel = booleanPreferencesKey("open_last_channel")
        val lastChannelId = stringPreferencesKey("last_channel_id")
    }

    private fun parseSectionIds(raw: String?): List<String> {
        return raw
            ?.split(SECTION_ORDER_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun encodeSectionIds(sectionIds: List<String>): String {
        return sectionIds
            .map { it.trim() }
            .filter { it.isNotBlank() && it in DEFAULT_VISIBLE_SECTION_IDS.toSet() }
            .distinct()
            .joinToString(SECTION_ORDER_SEPARATOR)
    }

    private fun normalizeSectionIds(sectionIds: List<String>): List<String> {
        val normalized = sectionIds
            .mapNotNull { id ->
                val trimmed = id.trim()
                if (trimmed in DEFAULT_VISIBLE_SECTION_IDS.toSet()) trimmed else null
            }
            .distinct()
        return normalized.ifEmpty { DEFAULT_VISIBLE_SECTION_IDS.toList() }
    }

    private companion object {
        const val SECTION_ORDER_SEPARATOR = ","
    }
}
