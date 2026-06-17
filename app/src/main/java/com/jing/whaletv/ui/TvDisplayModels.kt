package com.jing.whaletv.ui

import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvChannel
import java.util.Locale
import kotlin.math.absoluteValue

data class HomeCountryTabSpec(
    val id: String,
    val label: String,
    val locked: Boolean = false,
)

val HomeCountryTabs = listOf(
    HomeCountryTabSpec("cn", "中国", locked = true),
    HomeCountryTabSpec("us", "美国"),
    HomeCountryTabSpec("jp", "日本"),
    HomeCountryTabSpec("uk", "英国"),
    HomeCountryTabSpec("kr", "韩国"),
)

data class HomeCategorySpec(
    val id: String,
    val label: String,
)

val HomeCategorySpecs = listOf(
    HomeCategorySpec("all", "全部"),
    HomeCategorySpec("general", "综合"),
    HomeCategorySpec("news", "新闻"),
    HomeCategorySpec("sports", "体育"),
    HomeCategorySpec("movie", "电影"),
    HomeCategorySpec("music", "音乐"),
    HomeCategorySpec("kids", "少儿"),
    HomeCategorySpec("documentary", "纪录片"),
    HomeCategorySpec("entertainment", "娱乐"),
    HomeCategorySpec("uncategorized", "未分类"),
)

private const val HOME_OTHER_COUNTRY = "其他地区"

private val homeCountryLabelToId = mapOf(
    "中国" to "cn",
    "美国" to "us",
    "日本" to "jp",
    "英国" to "uk",
    "韩国" to "kr",
)

private val countryNameHints = mapOf(
    "united states" to "美国",
    "usa" to "美国",
    "america" to "美国",
    "us" to "美国",
    "japan" to "日本",
    "japanese" to "日本",
    "jp" to "日本",
    "united kingdom" to "英国",
    "uk" to "英国",
    "gb" to "英国",
    "england" to "英国",
    "korea" to "韩国",
    "south korea" to "韩国",
    "kr" to "韩国",
)

private val isoCountryCodeLookup = Locale.getISOCountries().toSet()
private val isoCountryDisplayNameLookup = Locale.getISOCountries().associate { code ->
    val country = Locale.Builder()
        .setRegion(code)
        .build()
        .getDisplayCountry(Locale.SIMPLIFIED_CHINESE)
    code.lowercase(Locale.ROOT) to country.ifBlank { code.uppercase(Locale.ROOT) }
}
private val bracketPattern = Regex("""\(([^)]+)\)""")
private val squarePattern = Regex("""\[([^\]]+)\]""")
private val codeLikePattern = Regex("""\b([A-Za-z]{2,3})\b""")
private val qualityResolutionPattern = Regex("""(\d{3,4})[pi]?""", RegexOption.IGNORE_CASE)
private val channelNumberPattern = Regex("""\d+""")

fun TvChannel.homeCountryId(): String {
    val idSuffix = id.substringAfterLast('.', "").lowercase(Locale.ROOT).trim('.')
    val suffixCountry = normalizeCountryCandidate(idSuffix)
    val resolvedCountry = suffixCountry ?: resolveCountryFromText("$id $name $groupTitle")
    resolvedCountry?.let { country ->
        homeCountryLabelToId[country]?.let { return it }
    }
    return if (
        idSuffix == "cn" ||
        id.lowercase(Locale.ROOT).contains(".cn") ||
        hasChineseChars("$name $groupTitle")
    ) {
        "cn"
    } else {
        "other"
    }
}

fun TvChannel.homeCountryLabel(): String {
    return HomeCountryTabs.firstOrNull { it.id == homeCountryId() }?.label ?: HOME_OTHER_COUNTRY
}

fun TvChannel.homeCategoryId(): String {
    val primary = groupTitle
        .split(';', ',', '|', '/', '，')
        .map { it.trim().lowercase(Locale.ROOT) }
        .firstOrNull { it.isNotBlank() }
        ?: return "uncategorized"
    return when (primary) {
        "general" -> "general"
        "news", "public" -> "news"
        "sports", "sport" -> "sports"
        "movies", "movie", "cinema", "classic" -> "movie"
        "music", "radio" -> "music"
        "kids", "children", "animation", "family" -> "kids"
        "documentary", "documentaries", "science", "education", "educational" -> "documentary"
        "entertainment", "comedy", "series" -> "entertainment"
        "undefined", "unknown", "other", "others" -> "uncategorized"
        else -> "uncategorized"
    }
}

fun TvChannel.homeCategoryLabel(): String {
    return HomeCategorySpecs.firstOrNull { it.id == homeCategoryId() }?.label ?: "未分类"
}

fun homeChannelsForCategory(categoryId: String, channels: List<TvChannel>): List<TvChannel> {
    return if (categoryId == "all") channels else channels.filter { it.homeCategoryId() == categoryId }
}

fun homeFavoriteChannels(channels: List<TvChannel>): List<TvChannel> {
    return channels.filter { it.isFavorite }
}

fun homeHistoryChannels(channels: List<TvChannel>): List<TvChannel> {
    return channels
        .filter { it.lastWatchedAt != null }
        .sortedByDescending { it.lastWatchedAt }
}

fun TvChannel.homePlayableSourceCount(): Int {
    return streams.count { it.healthStatus == StreamHealth.HEALTHY || it.healthStatus == StreamHealth.UNKNOWN }
}

fun TvChannel.homeQualityLabel(): String? {
    val qualities = streams.mapNotNull { it.quality?.trim() }.filter { it.isNotBlank() }
    if (qualities.any { it.contains("8K", ignoreCase = true) }) return "8K"
    if (qualities.any { it.contains("4K", ignoreCase = true) }) return "4K"
    val hasHd = qualities.any { quality ->
        val resolution = qualityResolutionPattern.find(quality)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        quality.equals("HD", ignoreCase = true) || resolution != null && resolution >= 720
    }
    return if (hasHd) "高清" else null
}

fun TvChannel.homeLogoPrimaryText(): String {
    val normalized = name.replace("高清", "").replace("频道", "").trim()
    return when {
        id.equals("CCTVPlus1.cn", ignoreCase = true) -> "新闻"
        id.equals("CCTVPlus2.cn", ignoreCase = true) -> "E"
        normalized.contains("CGTN", ignoreCase = true) -> "CGTN"
        normalized.contains("新华社") -> "新华社"
        normalized.contains("凤凰") -> "凤凰资讯"
        normalized.contains("CCTV-新闻", ignoreCase = true) || normalized.contains("CCTV新闻", ignoreCase = true) -> "新闻"
        normalized.contains("CCTV", ignoreCase = true) -> displayNumber()
        normalized.length >= 4 -> normalized.take(4)
        normalized.isNotBlank() -> normalized
        else -> logoText()
    }
}

fun TvChannel.homeLogoSecondaryText(): String? {
    val normalized = name.trim()
    return when {
        id.equals("CCTVPlus1.cn", ignoreCase = true) || id.equals("CCTVPlus2.cn", ignoreCase = true) -> "CCTV"
        normalized.contains("CCTV", ignoreCase = true) -> "CCTV"
        normalized.contains("新华社") -> "XINHUA"
        normalized.contains("凤凰") -> "PHOENIXTV"
        else -> null
    }
}

fun TvChannel.currentTitle(): String? = currentProgram?.title

private fun TvChannel.logoText(): String {
    val normalized = name.replace("高清", "").replace("频道", "").trim()
    return when {
        normalized.contains("CCTV-1") || normalized.contains("CCTV1") -> "综合"
        normalized.contains("CCTV-4") || normalized.contains("CCTV4") -> "国际"
        normalized.contains("CCTV-5") || normalized.contains("CCTV5") -> "体育"
        normalized.contains("CCTV-6") || normalized.contains("CCTV6") -> "电影"
        normalized.contains("CCTV-9") || normalized.contains("CCTV9") -> "纪录"
        normalized.contains("CCTV-13") || normalized.contains("CCTV13") -> "新闻"
        normalized.contains("CCTV-14") || normalized.contains("CCTV14") -> "少儿"
        normalized.contains("卫视") -> normalized.substringBefore("卫视").takeLast(2)
        normalized.length >= 2 -> normalized.take(2)
        else -> "电视"
    }
}

private fun TvChannel.displayNumber(): String {
    return channelNumberPattern.find(name)?.value
        ?: ((id.hashCode().absoluteValue % 90) + 10).toString()
}

private fun hasChineseChars(value: String): Boolean = value.any { it.code in 0x4E00..0x9FFF }

private fun resolveCountryFromText(value: String): String? {
    val normalized = value.lowercase(Locale.ROOT)
    countryNameHints.forEach { (hint, country) ->
        if (normalized.contains(hint)) return country
    }
    bracketPattern.findAll(value).forEach { match ->
        normalizeCountryCandidate(match.groupValues[1])?.let { return it }
    }
    squarePattern.findAll(value).forEach { match ->
        normalizeCountryCandidate(match.groupValues[1])?.let { return it }
    }
    codeLikePattern.findAll(value).forEach { match ->
        normalizeCountryCandidate(match.groupValues[1])?.let { return it }
    }
    return null
}

private fun normalizeCountryCandidate(candidate: String): String? {
    val compact = candidate.lowercase(Locale.ROOT).trim().replace(".", "")
    countryNameHints[compact]?.let { return it }
    if (compact.length in 2..3 && isCountryCodeCandidate(compact)) {
        return resolveCountryDisplayName(compact)
    }
    return null
}

private fun isCountryCodeCandidate(value: String): Boolean {
    val normalized = value.lowercase(Locale.ROOT)
    return normalized.length in 2..3 &&
        normalized.all { it.isLetter() } &&
        normalized.uppercase(Locale.ROOT) in isoCountryCodeLookup
}

private fun resolveCountryDisplayName(code: String): String {
    return isoCountryDisplayNameLookup[code.lowercase(Locale.ROOT)] ?: code.uppercase(Locale.ROOT)
}
