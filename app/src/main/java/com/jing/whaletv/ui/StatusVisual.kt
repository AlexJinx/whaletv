package com.jing.whaletv.ui

import androidx.compose.ui.graphics.Color
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.theme.WhaleTokens

/** 同步状态的统一展示映射（顶栏状态点 / 设置页 / 国家编辑页共用）。 */
data class SyncStatusVisual(
    val text: String,
    val color: Color,
)

fun syncStatusVisual(
    isRefreshing: Boolean,
    message: String?,
    syncSummary: SyncSummary,
): SyncStatusVisual {
    return when {
        isRefreshing -> SyncStatusVisual("正在同步", WhaleTokens.Accent)
        message != null -> SyncStatusVisual(message, WhaleTokens.Accent)
        syncSummary.playlistLastError != null -> SyncStatusVisual("同步失败", WhaleTokens.Red)
        syncSummary.playlistLastSuccessAt != null -> SyncStatusVisual("已同步", WhaleTokens.Green)
        else -> SyncStatusVisual("等待同步", WhaleTokens.TextSecondary)
    }
}
