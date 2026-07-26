package com.jing.whaletv.core

import java.util.Locale

/**
 * 首页国家入口的核心模型与归一化规则。
 * 放在 core 层：数据层（SettingsRepository）与 UI 层共同依赖，
 * 修复原先 data → ui 的反向依赖。
 */
data class HomeCountryTabSpec(
    val id: String,
    val label: String,
    val locked: Boolean = false,
)

const val MAX_HOME_COUNTRY_TABS = 20

val HomeCountryTabs = listOf(
    HomeCountryTabSpec("cn", "中国", locked = true),
    HomeCountryTabSpec("us", "美国"),
    HomeCountryTabSpec("jp", "日本"),
    HomeCountryTabSpec("uk", "英国"),
    HomeCountryTabSpec("kr", "韩国"),
)

/** 归一化：trim + 小写 + 去重，'cn' 强制存在且始终第一，上限 [MAX_HOME_COUNTRY_TABS]。 */
fun normalizeHomeCountryTabIds(ids: List<String>): List<String> {
    val cleaned = ids
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .distinct()
        .filterNot { it == "cn" }
    return (listOf("cn") + cleaned).take(MAX_HOME_COUNTRY_TABS)
}
