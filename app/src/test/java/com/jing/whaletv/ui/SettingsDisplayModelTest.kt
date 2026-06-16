package com.jing.whaletv.ui

import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.screens.epgCoverageText
import com.jing.whaletv.ui.screens.epgGuideCandidateText
import com.jing.whaletv.ui.screens.epgSampleChannelsText
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
}
