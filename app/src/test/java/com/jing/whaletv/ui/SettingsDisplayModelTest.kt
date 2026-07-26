package com.jing.whaletv.ui

import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.screens.settings.epgCoverageText
import com.jing.whaletv.ui.screens.settings.epgGuideCandidateText
import com.jing.whaletv.ui.screens.settings.epgSampleChannelsText
import com.jing.whaletv.ui.screens.settings.settingsEpgSourceState
import com.jing.whaletv.ui.screens.settings.settingsPlaylistSourceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun epgGuideCandidateText_showsGuideSourceCount() {
        assertEquals(
            "guides.json 2 个候选",
            epgGuideCandidateText(SyncSummary(epgGuideSourceCount = 2)),
        )
        assertEquals("guides.json 未发现", epgGuideCandidateText(SyncSummary(epgGuideSourceCount = 0)))
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
    fun settingsEpgSourceState_usesGuideCandidatesWithoutPlaylistUrl() {
        val state = settingsEpgSourceState(
            effectiveEpgUrl = null,
            syncSummary = SyncSummary(epgGuideSourceCount = 3),
        )

        assertEquals("来自 guides.json 官方候选", state.note)
        assertEquals("3 个候选节目单来源", state.value)
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

    @Test
    fun settingsPlaylistSourceState_showsScopedFallbackChainForChinaScope() {
        val state = settingsPlaylistSourceState(PlaylistScope.COUNTRY_CN)

        assertEquals("3 个来源 · 中国频道", state.note)
        assertTrue(state.value.contains("Gitee raw 镜像 → Gitee raw 镜像全量索引兜底 → iptv-org 官方源"))
        assertTrue(state.value.contains("当前优先：https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/countries/cn.m3u"))
    }

    @Test
    fun settingsPlaylistSourceState_keepsRegularScopeChainCompact() {
        val state = settingsPlaylistSourceState(PlaylistScope.CATEGORY_NEWS)

        assertEquals("2 个来源 · 新闻频道", state.note)
        assertTrue(state.value.contains("Gitee raw 镜像 → iptv-org 官方源"))
        assertTrue(state.value.contains("当前优先：https://gitee.com/AlexJinx/iptv-mirror/raw/pages/iptv/categories/news.m3u"))
        assertFalse(state.value.contains("全量索引兜底"))
    }
}
