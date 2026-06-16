package com.jing.whaletv.ui

import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.screens.epgCoverageText
import com.jing.whaletv.ui.screens.epgGuideCandidateText
import com.jing.whaletv.ui.screens.epgSampleChannelsText
import com.jing.whaletv.ui.screens.settingsEpgSourceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDisplayModelTest {
    @Test
    fun effectiveEpgUrl_usesDiscoveredUrlOnly() {
        val discovered = SyncSummary(discoveredEpgUrl = "https://example.com/discovered.xml")

        assertEquals(
            "https://example.com/discovered.xml",
            effectiveEpgUrl(discovered),
        )
        assertNull(effectiveEpgUrl(SyncSummary()))
        assertNull(effectiveEpgUrl(SyncSummary(discoveredEpgUrl = "")))
    }

    @Test
    fun epgCoverageText_usesRealProgramDiagnostics() {
        val diagnostics = SettingsDiagnostics(
            programCount = 128,
            epgChannelCount = 3,
            epgSampleChannelIds = listOf("AlJazeera.qa", "ANT1Europe.gr"),
        )

        assertEquals("3 个频道 · 128 条节目", epgCoverageText(diagnostics))
        assertEquals("AlJazeera.qa · ANT1Europe.gr", epgSampleChannelsText(diagnostics.epgSampleChannelIds))
        assertEquals("暂无可测试频道", epgSampleChannelsText(emptyList()))
    }

    @Test
    fun epgGuideCandidateText_usesOfficialDirectSourceCount() {
        assertEquals(
            "2 个直接来源",
            epgGuideCandidateText(SyncSummary(epgGuideSourceCount = 2)),
        )
    }

    @Test
    fun settingsEpgSourceState_prefersPlaylistUrlWhenAvailable() {
        val state = settingsEpgSourceState(
            effectiveEpgUrl = "https://example.com/playlist.xml",
            syncSummary = SyncSummary(epgGuideSourceCount = 3),
        )

        assertEquals("来自 playlist 自动发现", state.note)
        assertEquals("https://example.com/playlist.xml", state.value)
        assertEquals(true, state.canTest)
    }

    @Test
    fun settingsEpgSourceState_allowsGuideCandidatesWithoutPlaylistUrl() {
        val state = settingsEpgSourceState(
            effectiveEpgUrl = null,
            syncSummary = SyncSummary(epgGuideSourceCount = 3),
        )

        assertEquals("来自 iptv-org 官方候选", state.note)
        assertEquals("官方候选：3 个直接来源", state.value)
        assertEquals(true, state.canTest)
    }

    @Test
    fun settingsEpgSourceState_disablesTestingWhenNoSourceExists() {
        val state = settingsEpgSourceState(
            effectiveEpgUrl = null,
            syncSummary = SyncSummary(epgGuideSourceCount = 0),
        )

        assertEquals("尚未发现节目单地址", state.note)
        assertEquals("playlist 暂未发现 x-tvg-url", state.value)
        assertEquals(false, state.canTest)
    }
}
