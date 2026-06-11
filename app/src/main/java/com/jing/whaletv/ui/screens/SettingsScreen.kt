package com.jing.whaletv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jing.whaletv.data.model.ChannelSortMode
import com.jing.whaletv.data.model.DEFAULT_VISIBLE_SECTION_IDS
import com.jing.whaletv.ui.HomeUiState
import com.jing.whaletv.ui.TvNavSection
import com.jing.whaletv.ui.TvNavSections
import com.jing.whaletv.ui.components.TvIconButton
import com.jing.whaletv.ui.components.TvTextButton
import com.jing.whaletv.ui.theme.WhaleTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class SettingsSection(val id: String, val label: String)

private val settingsSections = listOf(
    SettingsSection("source", "直播地址"),
    SettingsSection("epg", "节目单"),
    SettingsSection("refresh", "刷新"),
    SettingsSection("display", "分类"),
    SettingsSection("playback", "播放"),
    SettingsSection("startup", "启动行为"),
    SettingsSection("cache", "缓存"),
)

@Composable
fun SettingsScreen(
    state: HomeUiState,
    onBack: () -> Unit,
    onCustomPlaylistChanged: (String) -> Unit,
    onXmltvChanged: (String) -> Unit,
    onAutoRefreshChanged: (Boolean) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onChannelSortModeChanged: (ChannelSortMode) -> Unit,
    onSectionOrderChanged: (List<String>) -> Unit,
    onOpenLastChannelChanged: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onRefreshNow: () -> Unit,
) {
    var activeSection by rememberSaveable { mutableStateOf("source") }
    var customPlaylist by remember(state.settings.customPlaylistUrl) { mutableStateOf(state.settings.customPlaylistUrl) }
    var xmltv by remember(state.settings.xmltvUrl) { mutableStateOf(state.settings.xmltvUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhaleTokens.Background),
    ) {
        SettingsHeader(onBack = onBack)
        Row(modifier = Modifier.fillMaxSize()) {
            SettingsNav(activeSection = activeSection, onSelect = { activeSection = it })
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
            ) {
                Column(modifier = Modifier.width(720.dp)) {
                    Text(
                        text = settingsSections.firstOrNull { it.id == activeSection }?.label ?: "设置",
                        color = WhaleTokens.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 18.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.06f)),
                    )
            when (activeSection) {
                "source" -> SourceSettings(
                    customPlaylist = customPlaylist,
                    onCustomPlaylistChange = { customPlaylist = it },
                    onSavePlaylist = { onCustomPlaylistChanged(customPlaylist) },
                )
                        "epg" -> EpgSettings(
                            state = state,
                            xmltv = xmltv,
                            onXmltvChange = { xmltv = it },
                            onSaveXmltv = { onXmltvChanged(xmltv) },
                            onRefreshNow = onRefreshNow,
                        )
                        "refresh" -> RefreshSettings(
                            state = state,
                            onAutoRefreshChanged = onAutoRefreshChanged,
                            onRefreshIntervalChanged = onRefreshIntervalChanged,
                            onRefreshNow = onRefreshNow,
                        )
                        "display" -> DisplaySettings(
                            state = state,
                            onChannelSortModeChanged = onChannelSortModeChanged,
                            onSectionOrderChanged = onSectionOrderChanged,
                        )
                        "playback" -> PlaybackSettings(state)
                        "startup" -> StartupSettings(
                            state = state,
                            onOpenLastChannelChanged = onOpenLastChannelChanged,
                        )
                        "cache" -> CacheSettings(
                            state = state,
                            onClearCache = onClearCache,
                            onRefreshNow = onRefreshNow,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(WhaleTokens.Sidebar)
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvIconButton(icon = Icons.Default.ArrowBack, contentDescription = "返回", onClick = onBack)
        Icon(Icons.Default.Settings, contentDescription = null, tint = WhaleTokens.SecondaryText, modifier = Modifier.size(18.dp))
        Text("设置", color = WhaleTokens.PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsNav(activeSection: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(WhaleTokens.Sidebar)
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .focusGroup()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        settingsSections.forEach { section ->
            var focused by remember { mutableStateOf(false) }
            val active = section.id == activeSection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .clickable { onSelect(section.id) }
                    .background(if (active || focused) Color(0x1400C8D4) else Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(if (active || focused) WhaleTokens.Cyan else Color.Transparent),
                )
                Text(
                    section.label,
                    color = if (active || focused) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 15.dp),
                )
            }
        }
    }
}

@Composable
private fun SourceSettings(
    customPlaylist: String,
    onCustomPlaylistChange: (String) -> Unit,
    onSavePlaylist: () -> Unit,
) {
    SettingsGroup("播放列表") {
        SettingRow(label = "内置地址", sublabel = "使用内置中国频道列表") {
            StatusText("启用", WhaleTokens.Green)
        }
        UrlRow(
            label = "自定义地址",
            value = customPlaylist,
            placeholder = "可选，用于添加自己的直播列表地址",
            onValueChange = onCustomPlaylistChange,
            onSave = onSavePlaylist,
        )
        SettingRow(
            label = "仅显示可观看频道",
            sublabel = "仅展示存在可播放线路的频道",
        ) {
            StatusText("开启", WhaleTokens.Green)
        }
    }
}

@Composable
private fun EpgSettings(
    state: HomeUiState,
    xmltv: String,
    onXmltvChange: (String) -> Unit,
    onSaveXmltv: () -> Unit,
    onRefreshNow: () -> Unit,
) {
    SettingsGroup("节目单") {
        UrlRow(
            label = "节目单地址",
            value = xmltv,
            placeholder = "可选，填入节目单地址",
            onValueChange = onXmltvChange,
            onSave = onSaveXmltv,
        )
        SettingRow(label = "节目单状态", sublabel = syncText(state.syncSummary.epgLastSuccessAt, "节目单未同步")) {
            val configured = xmltv.isNotBlank()
            val text = when {
                !configured -> "未配置"
                state.syncSummary.epgLastError != null -> "有错误"
                state.syncSummary.epgLastSuccessAt != null -> "已同步"
                else -> "等待"
            }
            val color = when {
                !configured -> WhaleTokens.SecondaryText
                state.syncSummary.epgLastError != null -> WhaleTokens.Red
                state.syncSummary.epgLastSuccessAt != null -> WhaleTokens.Green
                else -> WhaleTokens.SecondaryText
            }
            StatusText(text, color)
        }
        SettingRow(label = "更新节目单", sublabel = "和频道刷新一起执行") {
            TvTextButton(text = "立即更新", icon = Icons.Default.Refresh, onClick = onRefreshNow)
        }
    }
}

@Composable
private fun RefreshSettings(
    state: HomeUiState,
    onAutoRefreshChanged: (Boolean) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onRefreshNow: () -> Unit,
) {
    SettingsGroup("刷新") {
        ToggleRow("自动刷新", "后台定时更新频道列表", state.settings.autoRefresh, onAutoRefreshChanged)
        SettingRow(label = "刷新间隔", sublabel = "选择后台自动刷新频率") {
            SegmentedIntControl(
                options = listOf(6, 12, 24),
                value = state.settings.refreshIntervalHours,
                labelFor = { "${it}小时" },
                onChange = onRefreshIntervalChanged,
            )
        }
        SettingRow(label = "手动刷新", sublabel = "立即更新频道列表；如已配置节目单，会一起更新") {
            TvTextButton(text = "立即刷新", icon = Icons.Default.Refresh, onClick = onRefreshNow)
        }
    }
}

@Composable
private fun DisplaySettings(
    state: HomeUiState,
    onChannelSortModeChanged: (ChannelSortMode) -> Unit,
    onSectionOrderChanged: (List<String>) -> Unit,
) {
        SettingsGroup("分类") {
            SettingRow(label = "频道排序", sublabel = "调整各分区频道显示顺序") {
            SegmentedSortModeControl(
                options = ChannelSortMode.entries.toList(),
                value = state.settings.channelSortMode,
                labelFor = { sortModeLabel(it) },
                onChange = onChannelSortModeChanged,
            )
        }
        SettingRow(label = "主页分类", sublabel = "移除的分类不再显示在主页") {
            SectionVisibilityEditor(
                visibleSectionIds = state.settings.visibleSectionIds,
                onSectionOrderChanged = onSectionOrderChanged,
            )
        }
    }
}

@Composable
private fun SectionVisibilityEditor(
    visibleSectionIds: List<String>,
    onSectionOrderChanged: (List<String>) -> Unit,
) {
    val allSectionsById = TvNavSections.associateBy { it.id }
    val sectionIdSet = visibleSectionIds.toSet()
    val visibleSections = visibleSectionIds.mapNotNull { allSectionsById[it] }
    val hiddenSections = TvNavSections.filter { it.id !in sectionIdSet }
    val sectionIdsForCommit = visibleSections.map { it.id }

    if (sectionIdsForCommit.isEmpty()) {
        SectionVisibilityButton(
            text = "恢复默认分区",
            onClick = { onSectionOrderChanged(DEFAULT_VISIBLE_SECTION_IDS) },
            icon = Icons.Default.Add,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibleSections.forEach { section ->
            val canHide = visibleSections.size > 1
            SectionVisibilityRow(
                section = section,
                canHide = canHide,
                modifier = Modifier.focusGroup(),
                onHide = {
                    if (canHide) {
                        onSectionOrderChanged(sectionIdsForCommit.filterNot { it == section.id })
                    }
                },
            )
        }
        if (hiddenSections.isNotEmpty()) {
            Text(
                text = "已移除分类（可新增）",
                color = WhaleTokens.SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.focusGroup()) {
                hiddenSections.forEach { section ->
                SectionHiddenRow(
                    section = section,
                    onShow = { onSectionOrderChanged(sectionIdsForCommit + section.id) },
                )
                }
            }
        }
        SectionVisibilityButton(
            text = "恢复默认分类",
            icon = Icons.Default.Add,
            onClick = { onSectionOrderChanged(DEFAULT_VISIBLE_SECTION_IDS) },
        )
    }
}

@Composable
private fun SectionVisibilityRow(
    section: TvNavSection,
    canHide: Boolean,
    modifier: Modifier = Modifier,
    onHide: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        Column(modifier = Modifier.weight(1f).background(Color.Transparent), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.label, color = WhaleTokens.PrimaryText, fontSize = 13.sp, maxLines = 1)
            Text(section.id, color = WhaleTokens.SecondaryText, fontSize = 10.sp, maxLines = 1)
        }
        if (canHide) {
            TvTextButton(
                text = "−",
                icon = Icons.Default.Remove,
                onClick = onHide,
                modifier = Modifier.width(58.dp),
            )
        }
    }
}

@Composable
private fun SectionHiddenRow(section: TvNavSection, onShow: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.label, color = WhaleTokens.SecondaryText, fontSize = 13.sp, maxLines = 1)
            Text(section.id, color = WhaleTokens.SecondaryText.copy(alpha = 0.8f), fontSize = 10.sp, maxLines = 1)
        }
        TvTextButton(text = "＋", icon = Icons.Default.Add, onClick = onShow, modifier = Modifier.width(58.dp))
    }
}

@Composable
private fun SectionVisibilityButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    TvTextButton(text = text, icon = icon, primary = true, onClick = onClick, modifier = Modifier.width(150.dp))
}

@Composable
private fun PlaybackSettings(state: HomeUiState) {
    SettingsGroup("播放内核") {
        SettingRow(label = "播放内核", sublabel = "当前内核，兼容更多直播线路") {
            StatusText("内置", WhaleTokens.Cyan)
        }
        SettingRow(label = "备用线路切换", sublabel = "播放失败后自动尝试下一条线路") {
            StatusText("自动", WhaleTokens.Green)
        }
        SettingRow(label = "频道数量", sublabel = "当前可见频道") {
            StatusText("${state.channels.size}", WhaleTokens.PrimaryText)
        }
    }
}

@Composable
private fun StartupSettings(
    state: HomeUiState,
    onOpenLastChannelChanged: (Boolean) -> Unit,
) {
    SettingsGroup("启动行为") {
        ToggleRow("启动时播放上次频道", "应用启动后自动进入最后观看频道", state.settings.openLastChannel, onOpenLastChannelChanged)
        SettingRow(label = "启动屏幕", sublabel = "电视首屏保持频道浏览") {
            StatusText("主页", WhaleTokens.Cyan)
        }
    }
}

@Composable
private fun CacheSettings(
    state: HomeUiState,
    onClearCache: () -> Unit,
    onRefreshNow: () -> Unit,
) {
    SettingsGroup("缓存管理") {
        SettingRow(label = "频道缓存", sublabel = syncText(state.syncSummary.playlistLastSuccessAt, "频道未同步")) {
            StatusText("${state.channels.size} 个频道", WhaleTokens.PrimaryText)
        }
        SettingRow(label = "清除缓存", sublabel = "删除本地频道和节目单缓存") {
            TvTextButton(text = "清除", icon = Icons.Default.CleaningServices, onClick = onClearCache, focusedBorder = WhaleTokens.Red)
        }
        SettingRow(label = "重新同步", sublabel = "清理后可重新拉取远端数据") {
            TvTextButton(text = "刷新", icon = Icons.Default.Refresh, onClick = onRefreshNow)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = WhaleTokens.SecondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )
        content()
    }
}

@Composable
private fun ToggleRow(label: String, sublabel: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    SettingRow(label = label, sublabel = sublabel) {
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}


@Composable
private fun UrlRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(WhaleTokens.Surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Spacer(Modifier.weight(1f))
            TvTextButton(text = "保存", icon = Icons.Default.Save, primary = true, onClick = onSave)
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    sublabel: String? = null,
    trailing: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color(0x0D00C8D4) else Color.Transparent)
            .border(1.dp, if (focused) Color(0x3300C8D4) else Color.Transparent, RoundedCornerShape(6.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = WhaleTokens.PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    color = WhaleTokens.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing()
    }
}

@Composable
private fun SegmentedIntControl(
    options: List<Int>,
    value: Int,
    labelFor: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(WhaleTokens.Sidebar)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            SegmentButton(
                text = labelFor(option),
                selected = value == option,
                onClick = { onChange(option) },
            )
        }
    }
}

@Composable
private fun SegmentedSortModeControl(
    options: List<ChannelSortMode>,
    value: ChannelSortMode,
    labelFor: (ChannelSortMode) -> String,
    onChange: (ChannelSortMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(WhaleTokens.Sidebar)
            .border(1.dp, Color(0xFFF8F9FA).copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            SegmentButton(
                text = labelFor(option),
                selected = value == option,
                onClick = { onChange(option) },
            )
        }
    }
}

@Composable
private fun SegmentButton(text: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) WhaleTokens.Cyan else if (focused) Color(0x1F00C8D4) else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) WhaleTokens.Background else if (focused) WhaleTokens.Cyan else WhaleTokens.SecondaryText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(text = text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

private fun sortModeLabel(mode: ChannelSortMode): String {
    return when (mode) {
        ChannelSortMode.Default -> "默认"
        ChannelSortMode.NameAsc -> "名称"
        ChannelSortMode.LastWatched -> "最近播放"
        ChannelSortMode.Country -> "国家"
    }
}

private fun syncText(value: Long?, fallback: String): String {
    return value?.let {
        DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(it))
    } ?: fallback
}
