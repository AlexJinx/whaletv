package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.repository.SettingsRepository
import com.jing.whaletv.ui.components.TvFocusStyle
import com.jing.whaletv.ui.components.TvFocusable
import com.jing.whaletv.ui.theme.WhaleShapes
import com.jing.whaletv.ui.theme.WhaleTokens

@Composable
internal fun RefreshSettingsContent(
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    refreshInterval: Int,
    refreshIntervalText: String,
    onRefreshIntervalTextChange: (String) -> Unit,
    onRefreshIntervalStep: (Int) -> Unit,
) {
    SettingsCardStack {
        SettingsCardRow(height = SettingsBalancedRowHeight) {
            SettingsSwitchCard(
                title = "自动刷新",
                description = "按间隔同步 playlist 与 EPG",
                checked = autoRefresh,
                onCheckedChange = onAutoRefreshChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsIntervalCard(
                value = refreshInterval,
                text = refreshIntervalText,
                enabled = autoRefresh,
                onTextChange = onRefreshIntervalTextChange,
                onStep = onRefreshIntervalStep,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsHintText("自动刷新关闭后，顶部的立即刷新仍然可以手动同步频道和节目单。")
    }
}

@Composable
private fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 卡片聚焦时保持实底不透焦点填充，只亮描边，避免开关卡片底色发虚
    TvFocusable(
        onClick = { onCheckedChange(!checked) },
        shape = WhaleShapes.Button,
        style = TvFocusStyle(
            fill = WhaleTokens.SurfaceRaised,
            fillFocused = WhaleTokens.SurfaceRaised,
            border = WhaleTokens.Border,
        ),
        modifier = modifier,
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = WhaleTokens.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = WhaleTokens.TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WhaleTokens.Background,
                    checkedTrackColor = WhaleTokens.Accent,
                    checkedBorderColor = WhaleTokens.Accent,
                    uncheckedThumbColor = WhaleTokens.TextSecondary,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.16f),
                ),
            )
        }
    }
}

@Composable
private fun SettingsIntervalCard(
    value: Int,
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier, alpha = if (enabled) 1f else 0.56f) {
        SettingsCardTitle(
            title = "刷新间隔",
            description = "范围 ${SettingsRepository.MIN_REFRESH_INTERVAL_HOURS}-${SettingsRepository.MAX_REFRESH_INTERVAL_HOURS} 小时",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsStepButton(text = "-", enabled = enabled, onClick = { onStep(-1) })
            CompactSettingsInput(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                modifier = Modifier.width(78.dp),
                suffix = "小时",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            SettingsStepButton(text = "+", enabled = enabled, onClick = { onStep(1) })
        }
        Text("当前 $value 小时", color = WhaleTokens.TextTertiary, fontSize = 12.sp, maxLines = 1)
    }
}
