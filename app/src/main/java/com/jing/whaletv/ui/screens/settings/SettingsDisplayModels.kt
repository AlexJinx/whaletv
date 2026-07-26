package com.jing.whaletv.ui.screens.settings

import androidx.compose.ui.graphics.Color
import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.data.repository.playlistSourcesForScope
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun statusVisual(isRefreshing: Boolean, lastSuccessAt: Long?, error: String?): StatusVisual {
    return when {
        isRefreshing -> StatusVisual("刷新中", WhaleTokens.Accent)
        !error.isNullOrBlank() -> StatusVisual("失败", WhaleTokens.Red)
        lastSuccessAt != null -> StatusVisual("已同步", WhaleTokens.Green)
        else -> StatusVisual("未同步", WhaleTokens.TextSecondary)
    }
}

internal fun epgCoverageText(diagnostics: SettingsDiagnostics): String {
    return "${diagnostics.epgChannelCount} 个频道 · ${diagnostics.programCount} 条节目"
}

internal fun epgSampleChannelsText(channelIds: List<String>): String {
    return channelIds.take(6).joinToString(" · ").ifBlank { "暂无可测试频道" }
}

internal fun epgGuideCandidateText(syncSummary: SyncSummary): String {
    return if (syncSummary.epgGuideSourceCount > 0) {
        "guides.json ${syncSummary.epgGuideSourceCount} 个候选"
    } else {
        "guides.json 未发现"
    }
}

internal data class SettingsEpgSourceState(
    val note: String,
    val value: String,
    val canTest: Boolean,
)

internal data class SettingsPlaylistSourceState(
    val note: String,
    val value: String,
)

internal fun settingsPlaylistSourceState(playlistScope: PlaylistScope): SettingsPlaylistSourceState {
    val sources = playlistSourcesForScope(playlistScope)
    val sourceChain = sources.joinToString(" → ") { source -> source.label }
    val primaryUrl = sources.firstOrNull()?.url.orEmpty()
    return SettingsPlaylistSourceState(
        note = "${sources.size} 个来源 · ${playlistScope.label}",
        value = "$sourceChain\n当前优先：$primaryUrl",
    )
}

internal fun settingsEpgSourceState(effectiveEpgUrl: String?, syncSummary: SyncSummary): SettingsEpgSourceState {
    val playlistUrl = effectiveEpgUrl?.takeIf { it.isNotBlank() }
    return when {
        playlistUrl != null -> SettingsEpgSourceState(
            note = "来自 playlist 自动发现",
            value = playlistUrl,
            canTest = true,
        )
        syncSummary.epgGuideSourceCount > 0 -> SettingsEpgSourceState(
            note = "来自 guides.json 官方候选",
            value = "${syncSummary.epgGuideSourceCount} 个候选节目单来源",
            canTest = true,
        )
        else -> SettingsEpgSourceState(
            note = "尚未发现节目单地址",
            value = "playlist 暂未发现 x-tvg-url",
            canTest = false,
        )
    }
}

internal fun formatSyncTime(value: Long): String {
    return SyncTimeFormatter.format(Instant.ofEpochMilli(value))
}

internal data class StatusVisual(
    val text: String,
    val color: Color,
)

private val SyncTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
