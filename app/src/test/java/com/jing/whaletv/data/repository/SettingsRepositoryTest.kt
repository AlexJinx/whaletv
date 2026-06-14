package com.jing.whaletv.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jing.whaletv.data.model.AppSettings
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
            ),
        )
        val loaded = repository.settings.first()

        assertEquals(false, saved.autoRefresh)
        assertEquals(SettingsRepository.MAX_REFRESH_INTERVAL_HOURS, saved.refreshIntervalHours)
        assertEquals(saved, loaded)
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
