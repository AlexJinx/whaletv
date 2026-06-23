package com.jing.whaletv.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.ui.HomeCountryTabs
import com.jing.whaletv.ui.MAX_HOME_COUNTRY_TABS
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun saveSettings_persistsAutoRefreshAndClampedRefreshInterval() = runTest {
        val repository = repository("save_settings.preferences_pb")

        val saved = repository.saveSettings(
            AppSettings(
                autoRefresh = false,
                refreshIntervalHours = 99,
                playlistScope = PlaylistScope.LANGUAGE_ZHO,
            ),
        )
        val loaded = repository.settings.first()

        assertEquals(false, saved.autoRefresh)
        assertEquals(SettingsRepository.MAX_REFRESH_INTERVAL_HOURS, saved.refreshIntervalHours)
        assertEquals(PlaylistScope.LANGUAGE_ZHO, saved.playlistScope)
        assertEquals(HomeCountryTabs.map { it.id }, saved.homeCountryTabIds)
        assertEquals(saved, loaded)
    }

    @Test
    fun settings_defaultToAllPlaylistScope() = runTest {
        val repository = repository("default_scope.preferences_pb")

        assertEquals(PlaylistScope.ALL, repository.settings.first().playlistScope)
        assertEquals(HomeCountryTabs.map { it.id }, repository.settings.first().homeCountryTabIds)
    }

    @Test
    fun settings_invalidPlaylistScopeFallsBackToAll() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, "invalid_scope.preferences_pb") },
        )
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("playlist_scope")] = "not_supported"
        }
        val repository = SettingsRepository(dataStore)

        assertEquals(PlaylistScope.ALL, repository.settings.first().playlistScope)
    }

    @Test
    fun saveSettings_persistsHomeCountryTabIds() = runTest {
        val repository = repository("country_tabs.preferences_pb")

        val saved = repository.saveSettings(
            AppSettings(homeCountryTabIds = listOf("cn", "jp", "us")),
        )
        val loaded = repository.settings.first()

        assertEquals(listOf("cn", "jp", "us"), saved.homeCountryTabIds)
        assertEquals(saved.homeCountryTabIds, loaded.homeCountryTabIds)
    }

    @Test
    fun saveSettings_normalizesHomeCountryTabIds() = runTest {
        val repository = repository("normalize_country_tabs.preferences_pb")
        val manyCountryIds = ('a'..'z').take(MAX_HOME_COUNTRY_TABS + 4).map { "x$it" }

        val saved = repository.saveSettings(
            AppSettings(homeCountryTabIds = listOf("us", "jp", "us") + manyCountryIds),
        )

        assertEquals("cn", saved.homeCountryTabIds.first())
        assertEquals(MAX_HOME_COUNTRY_TABS, saved.homeCountryTabIds.size)
        assertEquals(1, saved.homeCountryTabIds.count { it == "us" })
    }

    @Test
    fun saveSettings_emptyHomeCountryTabIdsFallBackToDefaults() = runTest {
        val repository = repository("empty_country_tabs.preferences_pb")

        val saved = repository.saveSettings(AppSettings(homeCountryTabIds = emptyList()))

        assertEquals(HomeCountryTabs.map { it.id }, saved.homeCountryTabIds)
        assertEquals(HomeCountryTabs.map { it.id }, repository.settings.first().homeCountryTabIds)
    }

    @Test
    fun settings_invalidHomeCountryTabIdsFallBackToDefaults() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, "invalid_country_tabs.preferences_pb") },
        )
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("home_country_tab_ids")] = "not_supported,,123"
        }
        val repository = SettingsRepository(dataStore)

        assertEquals(HomeCountryTabs.map { it.id }, repository.settings.first().homeCountryTabIds)
    }

    @Test
    fun normalizeRefreshIntervalHours_clampsToSupportedRange() {
        assertEquals(1, SettingsRepository.normalizeRefreshIntervalHours(-4))
        assertEquals(12, SettingsRepository.normalizeRefreshIntervalHours(12))
        assertEquals(72, SettingsRepository.normalizeRefreshIntervalHours(120))
    }

    private fun TestScope.repository(fileName: String): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, fileName) },
        )
        return SettingsRepository(dataStore)
    }
}
