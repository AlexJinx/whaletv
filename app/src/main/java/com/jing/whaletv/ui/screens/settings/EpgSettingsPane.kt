package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary

@Composable
internal fun EpgSettingsContent(
    effectiveEpgUrl: String?,
    syncSummary: SyncSummary,
    diagnostics: SettingsDiagnostics,
    onTestActiveEpgSource: () -> Unit,
) {
    val epgSourceState = settingsEpgSourceState(effectiveEpgUrl, syncSummary)
    SettingsCardStack {
        SettingsCardRow(height = SettingsUrlRowHeight) {
            SourceStatusCard(
                title = "当前生效 EPG",
                note = epgSourceState.note,
                url = epgSourceState.value,
                enabled = epgSourceState.canTest,
                actionText = "测试节目单",
                onAction = onTestActiveEpgSource,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsCompactRowHeight) {
            SettingsValueCard(
                title = "真实覆盖",
                description = "来自本地已解析节目表",
                value = epgCoverageText(diagnostics),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsValueCard(
                title = "节目单来源",
                description = "playlist 自动发现 / guides.json 候选",
                value = epgGuideCandidateText(syncSummary),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsUrlRowHeight) {
            SettingsValueCard(
                title = "可测试频道",
                description = "当前真的有节目单",
                value = epgSampleChannelsText(diagnostics.epgSampleChannelIds),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsHintText("CCTV-13 这类频道如果没有 EPG 标签，表示当前公开节目单没有匹配到可解析数据。")
    }
}
