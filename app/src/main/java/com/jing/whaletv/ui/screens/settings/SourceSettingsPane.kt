package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.PlaylistScope
import com.jing.whaletv.data.model.SyncSummary
import com.jing.whaletv.ui.components.TvFocusable
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
internal fun SourceSettingsContent(
    playlistScope: PlaylistScope,
    syncSummary: SyncSummary,
    onPlaylistScopeChange: (PlaylistScope) -> Unit,
    effectiveEpgUrl: String?,
    onTestDefaultPlaylistSource: () -> Unit,
    onTestActiveEpgSource: () -> Unit,
) {
    val epgSourceState = settingsEpgSourceState(effectiveEpgUrl, syncSummary)
    val playlistSourceState = settingsPlaylistSourceState(playlistScope)
    SettingsCardStack {
        // 范围单选列表按条目数撑高，行高随内容取整行最小固有高度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(SettingsGridGap),
        ) {
            PlaylistScopeCard(
                selectedScope = playlistScope,
                onScopeChange = onPlaylistScopeChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsTextCard(
                title = "数据策略",
                description = "优先范围只使用官方预设路径，不提供手动输入源。同步时先请求 Gitee raw 镜像，失败后自动尝试 iptv-org 官方源。",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsUrlRowHeight) {
            SourceStatusCard(
                title = "优先 playlist",
                note = playlistSourceState.note,
                url = playlistSourceState.value,
                enabled = true,
                actionText = "测试源",
                onAction = onTestDefaultPlaylistSource,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsUrlRowHeight) {
            SourceStatusCard(
                title = "当前 EPG",
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
        SettingsHintText("保存后先更新所选范围，随后自动在后台补全全部频道；镜像不可用时会自动兜底官方源。")
    }
}

@Composable
private fun PlaylistScopeCard(
    selectedScope: PlaylistScope,
    onScopeChange: (PlaylistScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsCardTitle(title = "优先更新范围", description = selectedScope.description)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlaylistScope.entries.forEach { scope ->
                PlaylistScopeOptionRow(
                    scope = scope,
                    selected = scope == selectedScope,
                    onSelect = { onScopeChange(scope) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistScopeOptionRow(
    scope: PlaylistScope,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    TvFocusable(
        onClick = onSelect,
        selected = selected,
        shape = WhaleShapes.Button,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaylistScopeOptionHeight),
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(1.5.dp, if (selected) WhaleTokens.Accent else WhaleTokens.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(WhaleTokens.Accent, CircleShape),
                    )
                }
            }
            Text(
                text = scope.label,
                color = if (selected || focused) WhaleTokens.TextPrimary else WhaleTokens.TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val PlaylistScopeOptionHeight = 36.dp
