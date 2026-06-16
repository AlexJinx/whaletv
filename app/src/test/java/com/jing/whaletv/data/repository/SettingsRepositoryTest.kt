package com.jing.whaletv.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jing.whaletv.data.model.AppSettings
import com.jing.whaletv.data.model.PlaylistScope
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
        assertEquals(saved, loaded)
    }

    @Test
    fun settings_defaultToAllPlaylistScope() = runTest {
        val repository = repository("default_scope.preferences_pb")

        assertEquals(PlaylistScope.ALL, repository.settings.first().playlistScope)
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
