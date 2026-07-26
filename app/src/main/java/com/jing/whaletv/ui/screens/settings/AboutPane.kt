package com.jing.whaletv.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jing.whaletv.core.AppConstants

@Composable
internal fun AboutSourcesContent() {
    SettingsCardStack {
        SettingsCardRow(height = SettingsCompactRowHeight) {
            SettingsValueCard(
                title = "iptv-org playlist",
                description = "Gitee raw 镜像优先，官方源兜底",
                value = AppConstants.playlistUrls("index.m3u").joinToString(" · ") { it.url },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsValueCard(
                title = "EPG / API",
                description = "节目单与频道元数据来源",
                value = "https://github.com/iptv-org/epg · https://github.com/iptv-org/api",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        SettingsCardRow(height = SettingsBalancedRowHeight) {
            SettingsTextCard(
                title = "来源说明",
                description = "iptv-org 仓库本身不存储视频文件，只收集公开的直播链接；实际可用性会受频道源、地区和网络影响。",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SettingsTextCard(
                title = "优先更新范围",
                description = "可以在数据源页选择国家、语言或分类 playlist。每个范围都会先访问 Gitee raw 镜像，失败后再访问 iptv-org 官方源。",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}
