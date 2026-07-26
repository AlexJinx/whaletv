package com.jing.whaletv.ui

import com.jing.whaletv.data.model.TvChannel

// 首页网格内容的纯函数组装（无 Compose 依赖）：分类计数与分类内排序。
// 画质、源数等展示元数据统一来自 toChannelCardItem 的真实计算。

internal fun homeCategoryCounts(
    channels: List<TvChannel>,
    categories: List<HomeCategorySpec>,
): Map<String, Int> {
    val counts = categories.associate { it.id to 0 }.toMutableMap()
    counts["all"] = channels.size
    val visibleCategoryIds = categories.map { it.id }.toSet()
    channels.forEach { channel ->
        val categoryId = channel.homeCategoryId()
        if (categoryId in visibleCategoryIds) {
            counts[categoryId] = (counts[categoryId] ?: 0) + 1
        }
    }
    return counts
}

internal fun homeGridItemsForCategory(
    categoryId: String,
    countryChannels: List<TvChannel>,
): List<ChannelCardItem> {
    val categoryChannels = homeChannelsForCategory(categoryId, countryChannels)
    if (categoryId == "cctv") {
        return categoryChannels
            .sortedWith(homeCctvChannelComparator())
            .map { it.toChannelCardItem() }
    }
    if (categoryId == "satellite") {
        return categoryChannels
            .sortedWith(homeSatelliteChannelComparator())
            .map { it.toChannelCardItem() }
    }
    return categoryChannels.sortedForHomeBrowse().map { it.toChannelCardItem() }
}

internal fun List<TvChannel>.sortedForHomeBrowse(): List<TvChannel> {
    return sortedWith(homeBrowseChannelComparator())
}
