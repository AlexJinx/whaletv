package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
internal fun MaintenanceSettingsContent(
    diagnostics: SettingsDiagnostics,
    pendingAction: String?,
    onPendingAction: (String) -> Unit,
    onResetStreamHealth: () -> Unit,
    onClearEpgCache: () -> Unit,
    onClearWatchHistory: () -> Unit,
) {
    SettingsCardStack {
        SettingsCardRow(height = SettingsBalancedRowHeight) {
            MaintenanceActionCard(
                title = "重置播放源健康",
                description = "${diagnostics.unhealthyStreamCount} 个源当前标记不可用",
                actionId = MAINTENANCE_RESET_STREAMS,
                pendingAction = pendingAction,
                onPendingAction = onPendingAction,
                onConfirmed = onResetStreamHealth,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            MaintenanceActionCard(
                title = "清空节目单缓存",
                description = "当前缓存 ${diagnostics.programCount} 条节目",
                actionId = MAINTENANCE_CLEAR_EPG,
                pendingAction = pendingAction,
                onPendingAction = onPendingAction,
                onConfirmed = onClearEpgCache,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsBalancedRowHeight) {
            MaintenanceActionCard(
                title = "清空观看历史",
                description = "当前 ${diagnostics.historyCount} 个频道有历史",
                actionId = MAINTENANCE_CLEAR_HISTORY,
                pendingAction = pendingAction,
                onPendingAction = onPendingAction,
                onConfirmed = onClearWatchHistory,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsTextCard(
                title = "维护说明",
                description = "维护操作需要点击两次确认，只影响本地缓存、健康状态或观看记录，不删除默认 iptv-org 来源。",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsHintText("维护操作需要点击两次确认，只影响本地缓存和状态，不删除默认 iptv-org 源。")
    }
}

@Composable
private fun MaintenanceActionCard(
    title: String,
    description: String,
    actionId: String,
    pendingAction: String?,
    onPendingAction: (String) -> Unit,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirming = pendingAction == actionId
    SettingsCard(
        modifier = modifier,
        borderColor = if (confirming) WhaleTokens.Gold.copy(alpha = 0.70f) else null,
    ) {
        SettingsCardTitle(
            title = title,
            description = if (confirming) "再次点击确认执行" else description,
        )
        SettingsSmallButton(
            text = if (confirming) "确认" else "执行",
            highlighted = confirming,
            onClick = {
                if (confirming) {
                    onConfirmed()
                } else {
                    onPendingAction(actionId)
                }
            },
        )
    }
}

private const val MAINTENANCE_RESET_STREAMS = "reset_streams"
private const val MAINTENANCE_CLEAR_EPG = "clear_epg"
private const val MAINTENANCE_CLEAR_HISTORY = "clear_history"
