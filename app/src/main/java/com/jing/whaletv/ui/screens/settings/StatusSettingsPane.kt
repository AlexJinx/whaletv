package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.SettingsDiagnostics
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
internal fun SyncStatusContent(
    syncSummary: SyncSummary,
    diagnostics: SettingsDiagnostics,
    isRefreshing: Boolean,
) {
    SettingsCardStack {
        SettingsCardRow(height = SettingsStatusRowHeight) {
            SettingsStatusCard(
                label = "Playlist",
                description = "${diagnostics.playableChannelCount}/${diagnostics.channelCount} 个频道可播放 · ${diagnostics.streamCount} 个源",
                lastAttemptAt = syncSummary.playlistLastAttemptAt,
                lastSuccessAt = syncSummary.playlistLastSuccessAt,
                error = syncSummary.playlistLastError,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsStatusCard(
                label = "EPG",
                description = "${epgCoverageText(diagnostics)} · ${epgGuideCandidateText(syncSummary)}",
                lastAttemptAt = syncSummary.epgLastAttemptAt,
                lastSuccessAt = syncSummary.epgLastSuccessAt,
                error = syncSummary.epgLastError,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsStatusRowHeight) {
            SettingsStatusCard(
                label = "全量补全",
                description = "优先范围之后，后台恢复全部频道",
                lastAttemptAt = syncSummary.fullPlaylistLastAttemptAt,
                lastSuccessAt = syncSummary.fullPlaylistLastSuccessAt,
                error = syncSummary.fullPlaylistLastError,
                isRefreshing = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsMetricCard(
                title = "用户数据",
                value = "${diagnostics.favoriteCount} 收藏 · ${diagnostics.historyCount} 历史",
                description = "${diagnostics.unhealthyStreamCount} 个播放源标记不可用",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsHintText("最近尝试时间表示系统上一次启动同步任务；最近成功时间表示数据真正更新或确认未变化。")
    }
}

@Composable
private fun SettingsStatusCard(
    label: String,
    description: String,
    lastAttemptAt: Long?,
    lastSuccessAt: Long?,
    error: String?,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val visual = statusVisual(isRefreshing = isRefreshing, lastSuccessAt = lastSuccessAt, error = error)
    SettingsCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(visual.color),
            )
            Text(
                label,
                color = WhaleTokens.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusPill(text = visual.text, color = visual.color)
        }
        Text(
            description,
            color = WhaleTokens.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            StatusDetailRow(label = "尝试", value = lastAttemptAt?.let(::formatSyncTime) ?: "尚未")
            StatusDetailRow(label = "成功", value = lastSuccessAt?.let(::formatSyncTime) ?: "尚未")
            error?.takeIf { it.isNotBlank() }?.let {
                StatusDetailRow(label = "错误", value = it, valueColor = WhaleTokens.Red, maxLines = 2)
            }
        }
    }
}

@Composable
private fun SettingsMetricCard(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier) {
        SettingsCardTitle(title = title, description = description)
        Text(
            text = value,
            color = WhaleTokens.Accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusDetailRow(
    label: String,
    value: String,
    valueColor: Color = WhaleTokens.TextTertiary,
    maxLines: Int = 1,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = WhaleTokens.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(34.dp),
            maxLines = 1,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
