package com.jing.whaletv.ui

import com.jing.whaletv.data.model.TvChannel

data class ChannelCardItem(
    val key: String,
    val title: String,
    val categoryLabel: String,
    val logoLabel: String,
    val qualityLabel: String?,
    val sourceCount: Int,
    val hasEpg: Boolean,
    val currentProgramTitle: String?,
    val rank: Int,
)

fun TvChannel.toChannelCardItem(): ChannelCardItem {
    return ChannelCardItem(
        key = id,
        title = homeCardTitle(),
        categoryLabel = homeCategoryLabel(),
        logoLabel = homeLogoLabel(),
        qualityLabel = homeQualityLabel(),
        sourceCount = homePlayableSourceCount(),
        hasEpg = currentProgram != null || nextProgram != null || schedulePrograms.isNotEmpty(),
        currentProgramTitle = currentTitle(),
        rank = homeDesignRank(),
    )
}

fun TvChannel.homeDesignRank(): Int {
    val normalizedId = id.lowercase()
    val normalizedName = name.lowercase()
    return when {
        normalizedId == "cctv13.cn" || normalizedName.contains("cctv-13") || normalizedName.contains("cctv13") -> 0
        normalizedId == "cgtn.cn" || normalizedName == "cgtn" -> 1
        name.contains("新华社") -> 2
        name.contains("凤凰资讯") -> 3
        normalizedName.contains("cctv-新闻") || normalizedName.contains("cctv新闻") -> 4
        normalizedName.contains("cctv-4") || normalizedName.contains("cctv4") -> 5
        normalizedId == "cctv1.cn" || normalizedName == "cctv-1" -> 6
        normalizedId == "cctvplus1.cn" || normalizedName == "cctv+ 1" -> 7
        normalizedId == "cctvplus2.cn" || normalizedName == "cctv+ 2" -> 8
        name.contains("凤凰") -> 22
        normalizedName.contains("cctv") -> 30
        normalizedName.contains("cgtn") -> 40
        else -> 100
    }
}

private fun TvChannel.homeCardTitle(): String {
    val normalizedId = id.lowercase()
    val normalizedName = name.trim()
    return when {
        normalizedId == "cctv13.cn" || normalizedName == "CCTV-13" -> "CCTV-13 新闻"
        normalizedId == "cctv1.cn" || normalizedName == "CCTV-1" -> "CCTV-1 综合"
        normalizedId.startsWith("cctv4") && normalizedName.startsWith("CCTV-4") -> "CCTV-4 中文国际"
        normalizedId == "cctvplus1.cn" -> "CCTV-新闻"
        normalizedId == "cctvplus2.cn" -> "CCTV-英语"
        normalizedName == "CGTN" -> "CGTN"
        normalizedName.contains("凤凰资讯") -> "凤凰资讯"
        else -> normalizedName
    }
}

private fun TvChannel.homeLogoLabel(): String {
    val title = homeCardTitle().trim()
    title.toCctvLogoLabel()?.let { return it }

    val normalized = title
        .replace("高清", "")
        .replace("频道", "")
        .trim()
    return when {
        normalized.equals("CGTN", ignoreCase = true) -> "CGTN"
        normalized.contains("新华社") -> "新华社"
        normalized.contains("凤凰") -> "凤凰资讯"
        else -> normalized.ifBlank { name.trim() }
    }
}

private fun String.toCctvLogoLabel(): String? {
    val suffix = CctvLogoLabelPattern.find(this)?.groupValues?.getOrNull(1) ?: return null
    return if (suffix.equals("E", ignoreCase = true)) "CCTV-英语" else "CCTV-$suffix"
}

private val CctvLogoLabelPattern = Regex("""CCTV[\s\-+]*([0-9]{1,2}|新闻|英语|E)""", RegexOption.IGNORE_CASE)
