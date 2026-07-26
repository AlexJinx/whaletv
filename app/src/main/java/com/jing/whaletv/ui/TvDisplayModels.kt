package com.jing.whaletv.ui

import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.isPlaybackSupported
import java.util.Locale

// 核心模型已迁移至 core 层（供数据层共用）；ui 侧保留转发别名，调用点与测试不变。
typealias HomeCountryTabSpec = com.jing.whaletv.core.HomeCountryTabSpec

const val MAX_HOME_COUNTRY_TABS = com.jing.whaletv.core.MAX_HOME_COUNTRY_TABS

val HomeCountryTabs: List<HomeCountryTabSpec>
    get() = com.jing.whaletv.core.HomeCountryTabs

fun normalizeHomeCountryTabIds(ids: List<String>): List<String> =
    com.jing.whaletv.core.normalizeHomeCountryTabIds(ids)

data class CountryEntry(
    val id: String,
    val label: String,
    val channelCount: Int,
    val locked: Boolean = false,
)

data class HomeCategorySpec(
    val id: String,
    val label: String,
)

val HomeCategorySpecs = listOf(
    HomeCategorySpec("all", "全部"),
    HomeCategorySpec("cctv", "央视"),
    HomeCategorySpec("satellite", "卫视"),
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

fun homeCategorySpecsForCountry(countryId: String): List<HomeCategorySpec> {
    if (countryId == "cn") return HomeCategorySpecs
    return HomeCategorySpecs.filterNot { it.id == "cctv" || it.id == "satellite" }
}

fun normalizeHomeCategoryIdForCountry(countryId: String, categoryId: String): String {
    val visibleCategoryIds = homeCategorySpecsForCountry(countryId).map { it.id }.toSet()
    return if (categoryId in visibleCategoryIds) categoryId else "all"
}

private const val HOME_OTHER_COUNTRY = "其他地区"

private val countryHintIds = mapOf(
    "china" to "cn",
    "chinese" to "cn",
    "cn" to "cn",
    "united states" to "us",
    "usa" to "us",
    "america" to "us",
    "us" to "us",
    "japan" to "jp",
    "japanese" to "jp",
    "jp" to "jp",
    "united kingdom" to "uk",
    "uk" to "uk",
    "gb" to "uk",
    "england" to "uk",
    "korea" to "kr",
    "south korea" to "kr",
    "kr" to "kr",
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
private val cctvNumberPattern = Regex("""\bcctv\s*[- ]?\s*(\d{1,2})(?!\d)""", RegexOption.IGNORE_CASE)

fun TvChannel.homeCountryId(): String {
    val idSuffix = id.substringAfterLast('.', "").lowercase(Locale.ROOT).trim('.')
    val suffixCountryId = normalizeCountryIdCandidate(idSuffix)
    val resolvedCountryId = suffixCountryId ?: resolveCountryIdFromText("$id $name $groupTitle")
    resolvedCountryId?.let { return it }
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
    return homeCountryLabelForId(homeCountryId())
}

fun homeCountryEntries(channels: List<TvChannel>): List<CountryEntry> {
    val counts = channels.groupingBy { it.homeCountryId() }.eachCount()
    val ids = (HomeCountryTabs.map { it.id } + counts.keys)
        .distinct()
        .filter { it.isNotBlank() }
    return ids
        .map { id ->
            CountryEntry(
                id = id,
                label = homeCountryLabelForId(id),
                channelCount = counts[id] ?: 0,
                locked = id == "cn",
            )
        }
        .sortedWith(
            compareBy<CountryEntry> { defaultHomeCountryOrder(it.id) }
                .thenByDescending { it.channelCount }
                .thenBy { it.label },
        )
}

fun homeCountryTabsForIds(ids: List<String>, channels: List<TvChannel>): List<HomeCountryTabSpec> {
    val availableIds = homeCountryEntries(channels).map { it.id }.toSet()
    val normalizedIds = normalizeHomeCountryTabIds(ids)
    return normalizedIds
        .filter { it in availableIds || it in HomeCountryTabs.map(HomeCountryTabSpec::id) }
        .take(MAX_HOME_COUNTRY_TABS)
        .ifEmpty { HomeCountryTabs.map { it.id } }
        .map { id ->
            HomeCountryTabSpec(
                id = id,
                label = homeCountryLabelForId(id),
                locked = id == "cn",
            )
        }
}

fun addHomeCountryTab(ids: List<String>, id: String): List<String> {
    val normalized = normalizeHomeCountryTabIds(ids)
    val cleanedId = id.trim().lowercase(Locale.ROOT)
    if (cleanedId.isBlank() || cleanedId in normalized || normalized.size >= MAX_HOME_COUNTRY_TABS) {
        return normalized
    }
    return normalizeHomeCountryTabIds(normalized + cleanedId)
}

fun removeHomeCountryTab(ids: List<String>, id: String): List<String> {
    val normalized = normalizeHomeCountryTabIds(ids)
    val cleanedId = id.trim().lowercase(Locale.ROOT)
    if (cleanedId == "cn") return normalized
    return normalizeHomeCountryTabIds(normalized.filterNot { it == cleanedId })
}

fun moveHomeCountryTab(ids: List<String>, id: String, direction: Int): List<String> {
    val normalized = normalizeHomeCountryTabIds(ids)
    val cleanedId = id.trim().lowercase(Locale.ROOT)
    if (cleanedId == "cn") return normalized
    val from = normalized.indexOf(cleanedId)
    if (from <= 0) return normalized
    val target = (from + direction).coerceIn(1, normalized.lastIndex)
    if (target == from) return normalized
    val updated = normalized.toMutableList()
    updated.removeAt(from)
    updated.add(target, cleanedId)
    return normalizeHomeCountryTabIds(updated)
}

fun addableHomeCountryEntries(
    entries: List<CountryEntry>,
    visibleIds: List<String>,
    query: String,
): List<CountryEntry> {
    val visibleIdSet = normalizeHomeCountryTabIds(visibleIds).toSet()
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    return entries.filterNot { it.id in visibleIdSet }
        .filter { country ->
            normalizedQuery.isBlank() ||
                country.label.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                country.id.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
}

fun homeCountryLabelForId(id: String): String {
    HomeCountryTabs.firstOrNull { it.id == id }?.let { return it.label }
    if (id == "other") return HOME_OTHER_COUNTRY
    return isoCountryDisplayNameLookup[id.lowercase(Locale.ROOT)]
        ?: id.uppercase(Locale.ROOT)
}

fun TvChannel.homeCategoryId(): String {
    if (isCctvChannel()) return "cctv"
    if (isSatelliteChannel()) return "satellite"

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
    return when (categoryId) {
        "all" -> channels
        "cctv" -> channels.filter { it.isCctvChannel() }
        "satellite" -> channels.filter { it.isSatelliteChannel() && !it.isCctvChannel() }
        else -> channels.filter { it.homeCategoryId() == categoryId }
    }
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

fun TvChannel.homePlaybackSortRank(): Int {
    val supportedStreams = streams.filter { it.isPlaybackSupported() }
    return when {
        supportedStreams.any { it.healthStatus == StreamHealth.HEALTHY } -> 0
        supportedStreams.any { it.healthStatus == StreamHealth.UNKNOWN } -> 1
        else -> 2
    }
}

fun homeBrowseChannelComparator(): Comparator<TvChannel> {
    return compareBy<TvChannel> { it.homePlaybackSortRank() }
        .thenBy { it.homeDesignRank() }
        .thenBy { it.priority }
        .thenBy { it.name }
}

fun homeCctvChannelComparator(): Comparator<TvChannel> {
    return compareBy<TvChannel> { it.cctvSortKey() }
        .thenBy { it.homePlaybackSortRank() }
        .thenBy { it.name.lowercase(Locale.ROOT) }
}

fun homeSatelliteChannelComparator(): Comparator<TvChannel> {
    return compareBy<TvChannel> { it.name.lowercase(Locale.ROOT) }
        .thenBy { it.homePlaybackSortRank() }
        .thenBy { it.id.lowercase(Locale.ROOT) }
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

fun TvChannel.currentTitle(): String? = currentProgram?.title

fun TvChannel.isCctvChannel(): Boolean {
    val normalized = "$id $name".lowercase(Locale.ROOT)
    return normalized.contains("cctv") ||
        name.contains("央视") ||
        name.contains("中央电视台")
}

fun TvChannel.isSatelliteChannel(): Boolean {
    val normalized = "$id $name $groupTitle".lowercase(Locale.ROOT)
    return name.contains("卫视") ||
        normalized.contains("satellite tv") ||
        normalized.contains("satellitetv") ||
        normalized.contains("satellite channel")
}

fun TvChannel.cctvSortKey(): Int {
    val value = "${id.substringBefore('@')} $name"
    return cctvNumberPattern.find(value)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: Int.MAX_VALUE
}

private fun hasChineseChars(value: String): Boolean = value.any { it.code in 0x4E00..0x9FFF }

private fun resolveCountryIdFromText(value: String): String? {
    val normalized = value.lowercase(Locale.ROOT)
    countryHintIds.forEach { (hint, id) ->
        if (containsCountryHint(normalized, hint)) return id
    }
    bracketPattern.findAll(value).forEach { match ->
        normalizeCountryIdCandidate(match.groupValues[1])?.let { return it }
    }
    squarePattern.findAll(value).forEach { match ->
        normalizeCountryIdCandidate(match.groupValues[1])?.let { return it }
    }
    codeLikePattern.findAll(value).forEach { match ->
        normalizeCountryIdCandidate(match.groupValues[1])?.let { return it }
    }
    return null
}

private fun containsCountryHint(value: String, hint: String): Boolean {
    if (hint.length > 2) return value.contains(hint)
    var startIndex = value.indexOf(hint)
    while (startIndex >= 0) {
        val before = value.getOrNull(startIndex - 1)
        val after = value.getOrNull(startIndex + hint.length)
        if (before?.isLetterOrDigit() != true && after?.isLetterOrDigit() != true) return true
        startIndex = value.indexOf(hint, startIndex + 1)
    }
    return false
}

private fun normalizeCountryIdCandidate(candidate: String): String? {
    val compact = candidate.lowercase(Locale.ROOT).trim().replace(".", "")
    countryHintIds[compact]?.let { return it }
    if (compact.length == 2 && compact.uppercase(Locale.ROOT) in isoCountryCodeLookup) {
        return compact
    }
    return null
}

private fun defaultHomeCountryOrder(id: String): Int {
    val index = HomeCountryTabs.indexOfFirst { it.id == id }
    return if (index >= 0) index else Int.MAX_VALUE
}
