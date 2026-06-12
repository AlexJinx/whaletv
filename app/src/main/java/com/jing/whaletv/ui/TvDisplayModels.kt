package com.jing.whaletv.ui

import androidx.compose.ui.graphics.Color
import com.jing.whaletv.data.model.ChannelSortMode
import com.jing.whaletv.data.model.Program
import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.isPlayable
import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.parser.ChannelClassifier
import com.jing.whaletv.ui.theme.WhaleTokens
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

data class TvNavSection(
    val id: String,
    val label: String,
)

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

val TvNavSections = listOf(
    TvNavSection("continue", "继续观看"),
    TvNavSection("favorite", "收藏"),
    TvNavSection("cctv", "央视"),
    TvNavSection("satellite", "卫视"),
    TvNavSection("international", "国际"),
    TvNavSection("local", "地方"),
    TvNavSection("news", "新闻"),
    TvNavSection("sports", "体育"),
    TvNavSection("entertainment", "娱乐"),
    TvNavSection("movie", "电影"),
    TvNavSection("music", "音乐"),
    TvNavSection("lifestyle", "生活"),
    TvNavSection("kids", "少儿"),
    TvNavSection("documentary", "纪录片"),
    TvNavSection("all", "全部频道"),
)

const val INTERNATIONAL_ALL_COUNTRIES = "全部国家"
const val INTERNATIONAL_ALL_TYPES = "全部类型"
private const val INTERNATIONAL_OTHER_COUNTRY = "其他地区"

private val INTERNATIONAL_COUNTRY_PRIORITY = listOf(
    "美国",
    "英国",
    "日本",
    "韩国",
    "法国",
    "德国",
    "加拿大",
    "澳大利亚",
    "西班牙",
    "意大利",
    "巴西",
    "阿根廷",
    "印度",
    "新加坡",
    "以色列",
    "荷兰",
    "瑞典",
    "瑞士",
    "葡萄牙",
    "乌克兰",
    "挪威",
    "芬兰",
    "丹麦",
    "墨西哥",
    "南非",
    "巴基斯坦",
    "台湾",
    "香港",
    "澳门",
    "阿富汗",
    "阿联酋",
    "秘鲁",
    "哥伦比亚",
    "阿曼",
    "尼泊尔",
)

private val homeCountryLabelToId = mapOf(
    "中国" to "cn",
    "美国" to "us",
    "日本" to "jp",
    "英国" to "uk",
    "韩国" to "kr",
)

private val internationalCountryNameHints = mapOf(
    "united states" to "美国",
    "usa" to "美国",
    "america" to "美国",
    "canada" to "加拿大",
    "uk" to "英国",
    "united kingdom" to "英国",
    "england" to "英国",
    "scotland" to "英国",
    "ireland" to "爱尔兰",
    "france" to "法国",
    "germany" to "德国",
    "deutschland" to "德国",
    "italy" to "意大利",
    "spain" to "西班牙",
    "portugal" to "葡萄牙",
    "greece" to "希腊",
    "russia" to "俄罗斯",
    "turkey" to "土耳其",
    "japan" to "日本",
    "japanese" to "日本",
    "korea" to "韩国",
    "south korea" to "韩国",
    "india" to "印度",
    "singapore" to "新加坡",
    "australia" to "澳大利亚",
    "new zealand" to "新西兰",
    "mexico" to "墨西哥",
    "brazil" to "巴西",
    "argentina" to "阿根廷",
    "chile" to "智利",
    "norway" to "挪威",
    "sweden" to "瑞典",
    "finland" to "芬兰",
    "denmark" to "丹麦",
    "poland" to "波兰",
    "israel" to "以色列",
    "iran" to "伊朗",
    "south africa" to "南非",
    "africa" to "南非",
    "islamabad" to "巴基斯坦",
    "pakistan" to "巴基斯坦",
    "thailand" to "泰国",
    "vietnam" to "越南",
    "malaysia" to "马来西亚",
    "philippines" to "菲律宾",
    "hong kong" to "香港",
    "taiwan" to "台湾",
    "macau" to "澳门",
    "afghanistan" to "阿富汗",
    "austria" to "奥地利",
    "belgium" to "比利时",
    "czech" to "捷克",
    "czech republic" to "捷克",
    "holland" to "荷兰",
    "netherlands" to "荷兰",
    "norway" to "挪威",
    "switzerland" to "瑞士",
    "ukraine" to "乌克兰",
    "peru" to "秘鲁",
    "colombia" to "哥伦比亚",
    "oman" to "阿曼",
    "nepal" to "尼泊尔",
    "uae" to "阿联酋",
    "emirates" to "阿联酋",
)

private val isoCountryCodeLookup = Locale.getISOCountries().toSet()

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
    return HomeCountryTabs.firstOrNull { it.id == homeCountryId() }?.label ?: INTERNATIONAL_OTHER_COUNTRY
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
        val resolution = Regex("""(\d{3,4})[pi]?""", RegexOption.IGNORE_CASE)
            .find(quality)
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

fun TvChannel.resolvedInternationalCountry(): String {
    if (!isInternational()) return INTERNATIONAL_OTHER_COUNTRY
    val idCountry = resolveCountryFromId(id)
    if (idCountry != null) return idCountry
    val nameCountry = resolveCountryFromText(name)
    if (nameCountry != null) return nameCountry
    val groupCountry = resolveCountryFromText(groupTitle)
    if (groupCountry != null) return groupCountry
    return INTERNATIONAL_OTHER_COUNTRY
}

fun internationalCountriesForChannels(channels: List<TvChannel>): List<String> {
    return channels
        .asSequence()
        .map { it.resolvedInternationalCountry() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedWith(
            compareBy<String> { it == INTERNATIONAL_OTHER_COUNTRY }
                .thenBy { countryPriorityRank(it) }
                .thenBy { it.lowercase(Locale.ROOT) },
        )
        .toList()
}

private fun countryPriorityRank(name: String): Int {
    if (name == INTERNATIONAL_OTHER_COUNTRY) return Int.MAX_VALUE
    val index = INTERNATIONAL_COUNTRY_PRIORITY.indexOf(name)
    return if (index >= 0) index else INTERNATIONAL_COUNTRY_PRIORITY.size + name.length
}

fun internationalTypeBucketsForChannels(channels: List<TvChannel>): List<String> {
    if (channels.isEmpty()) return emptyList()
    val buckets = channels
        .groupingBy { it.channelSectionBucket() }
        .eachCount()
        .keys
    val order = listOf(
        "news",
        "sports",
        "entertainment",
        "movie",
        "documentary",
        "lifestyle",
        "music",
        "kids",
        "international",
        "other",
    )
    val ordered = order.filter { it in buckets }
    val rest = buckets.filterNot { it in order }.sorted()
    return ordered + rest
}

fun internationalTypeLabel(bucket: String): String {
    return when (bucket) {
        "news" -> "新闻"
        "sports" -> "体育"
        "entertainment" -> "娱乐"
        "movie" -> "电影"
        "documentary" -> "纪录片"
        "lifestyle" -> "生活"
        "music" -> "音乐"
        "kids" -> "少儿"
        "local" -> "地方"
        "satellite" -> "卫视"
        "cctv" -> "央视"
        "international" -> "综合"
        else -> "其他"
    }
}

fun TvChannel.resolvedInternationalTypeBucket(): String {
    return channelSectionBucket()
}

fun relatedPlayableChannels(current: TvChannel, channels: List<TvChannel>, limit: Int = 24): List<TvChannel> {
    val playable = channels
        .filter { it.isPlayable() && it.id != current.id }

    val sameGroup = playable.filter { it.groupTitle == current.groupTitle }
    val sameBucket = playable.filter { it.channelSectionBucket() == current.channelSectionBucket() }
    val international = if (current.isInternational()) {
        playable.filter { it.isInternational() }
    } else {
        playable.filter { it.channelSectionBucket() == "international" }
    }

    return (sameGroup + sameBucket + international + playable)
        .distinctBy { it.id }
        .take(limit)
}

fun channelsForSection(
    sectionId: String,
    channels: List<TvChannel>,
    channelSortMode: ChannelSortMode = ChannelSortMode.Default,
): List<TvChannel> {
    val playableChannels = channels.filter { it.isPlayable() }
    val playableSorted = when (channelSortMode) {
        ChannelSortMode.Default -> playableChannels
        ChannelSortMode.NameAsc -> playableChannels.sortedBy { it.name.lowercase() }
        ChannelSortMode.LastWatched -> playableChannels.sortedWith(
            compareByDescending<TvChannel> { it.lastWatchedAt ?: Long.MIN_VALUE }
                .thenBy { it.name.lowercase() },
        )
        ChannelSortMode.Country -> playableChannels.sortedWith(
            compareBy<TvChannel> { it.sortingCountryKey() }
                .thenBy { it.name.lowercase() },
        )
    }
    val playableCctv = playableChannels.filter { isCctv(it) }
    val playableSatellite = playableChannels.filter { isSatellite(it) && it !in playableCctv }
    val playableLocal = playableChannels.filter { isLocal(it) && it !in playableCctv && it !in playableSatellite }
    return when (sectionId) {
        "continue" -> playableSorted
            .filter { it.lastWatchedAt != null }
            .sortedByDescending { it.lastWatchedAt }
            .ifEmpty { playableSorted }
            .take(12)
        "favorite" -> playableSorted.filter { it.isFavorite }
        "cctv" -> playableSorted.filter { isCctv(it) }
        "satellite" -> playableSorted.filter { isSatellite(it) && it !in playableCctv }
        "international" -> playableSorted.filter { it.channelSectionBucket() == "international" }
        "local" -> playableSorted.filter { isLocal(it) && it !in playableCctv && it !in playableSatellite }
        "news" -> playableSorted.filter { it.channelSectionBucket() == "news" }
        "sports" -> playableSorted.filter { it.channelSectionBucket() == "sports" }
        "entertainment" -> playableSorted.filter { it.channelSectionBucket() == "entertainment" }
        "movie" -> playableSorted.filter { it.channelSectionBucket() == "movie" }
        "music" -> playableSorted.filter { it.channelSectionBucket() == "music" }
        "lifestyle" -> playableSorted.filter { it.channelSectionBucket() == "lifestyle" }
        "kids" -> playableSorted.filter { it.channelSectionBucket() == "kids" }
        "documentary" -> playableSorted.filter { it.channelSectionBucket() == "documentary" }
        else -> playableSorted
    }
}

private fun TvChannel.sortingCountryKey(): String {
    if (isInternational()) return resolvedInternationalCountry()
    return displayGroupTitle("频道")
}

fun TvChannel.logoText(): String {
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

fun TvChannel.logoColor(): Color {
    return when {
        isCctv(this) -> Color(0xFFC41E3A)
        name.contains("体育") || groupTitle.equals("Sports", true) -> Color(0xFF0B5EA8)
        name.contains("新闻") || groupTitle.equals("News", true) -> WhaleTokens.Red
        name.contains("少儿") || groupTitle.equals("Kids", true) -> Color(0xFFF59E0B)
        name.contains("纪录") || groupTitle.equals("Documentary", true) -> Color(0xFF2E7D32)
        isSatellite(this) -> Color(0xFF8A1253)
        isLocal(this) -> Color(0xFFB45309)
        else -> palette[(id.hashCode().absoluteValue) % palette.size]
    }
}

fun TvChannel.displayNumber(): String {
    return Regex("""\d+""").find(name)?.value
        ?: ((id.hashCode().absoluteValue % 90) + 10).toString()
}

fun TvChannel.displayGroupTitle(fallback: String = "频道"): String = groupTitle.toChineseGroupTitle(fallback)

fun String.toChineseGroupTitle(fallback: String = "频道"): String {
    val raw = trim()
    if (raw.isBlank()) return fallback
    val primary = raw
        .split(';', ',', '|', '/', '，')
        .map { it.trim().lowercase(Locale.ROOT) }
        .firstOrNull { it.isNotBlank() }
        ?: fallback
    return when (primary) {
        "undefined", "unknown", "other", "others" -> fallback
        "general" -> "综合"
        "news" -> "新闻"
        "sports" -> "体育"
        "kids", "children" -> "少儿"
        "lifestyle" -> "生活"
        "documentary", "documentaries" -> "纪录片"
        "education", "educational" -> "教育"
        "music" -> "音乐"
        "movies", "movie", "cinema" -> "电影"
        "entertainment" -> "娱乐"
        "business" -> "财经"
        "travel" -> "旅游"
        "weather" -> "天气"
        "shopping", "shop" -> "购物"
        "religion", "religious" -> "宗教"
        "family" -> "家庭"
        "religious", "religion" -> "宗教"
        "legislative", "government" -> "政务"
        else -> if (raw.any { it.code > 127 }) raw else fallback
    }
}

fun TvChannel.currentTitle(): String? = currentProgram?.title

fun TvChannel.nextTitle(): String? = nextProgram?.title

fun TvChannel.hasEpgData(): Boolean = currentProgram != null || nextProgram != null

fun TvChannel.currentTimeRange(): String? {
    val program = currentProgram ?: return null
    return "${formatProgramTime(program.startAt)} - ${formatProgramTime(program.endAt)}"
}

fun TvChannel.currentProgress(now: Long = System.currentTimeMillis()): Float? {
    val program = currentProgram ?: return null
    return programProgress(program, now)
}

fun formatProgramTime(value: Long): String {
    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}

fun programProgress(program: Program, now: Long = System.currentTimeMillis()): Float {
    val total = (program.endAt - program.startAt).coerceAtLeast(1L)
    return ((now - program.startAt).toFloat() / total).coerceIn(0.05f, 0.98f)
}

fun formatShortDate(value: Long): String {
    return DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}

private fun isCctv(channel: TvChannel): Boolean = ChannelClassifier.isCctv(channel.id, channel.name)

private fun isSatellite(channel: TvChannel): Boolean = ChannelClassifier.isSatellite(channel.name)

private fun isLocal(channel: TvChannel): Boolean {
    if (isCctv(channel) || isSatellite(channel) || isNews(channel) || isSports(channel)) return false
    if (channel.groupMatches("local", "local tv", "regional", "province", "city", "municipal")) return true
    val nameText = "${channel.name} ${channel.groupTitle}".lowercase(Locale.ROOT)
    return localizedRegionHints().any { hint -> nameText.contains(hint.lowercase()) }
}

private fun isNews(channel: TvChannel): Boolean {
    if (channel.groupMatches("news", "public", "新闻", "current affairs", "current_affairs", "headlines")) return true
    return channel.name.contains("新闻", ignoreCase = true) ||
        channel.name.contains("news", ignoreCase = true) ||
        channel.name.contains("报导", ignoreCase = true)
}

private fun isSports(channel: TvChannel): Boolean {
    if (channel.groupMatches("sports", "sport", "football", "basketball")) return true
    return channel.name.contains("体育", ignoreCase = true) ||
        channel.name.contains("sport", ignoreCase = true) ||
        channel.name.contains("足球", ignoreCase = true) ||
        channel.name.contains("篮球", ignoreCase = true)
}

private fun isEntertainment(channel: TvChannel): Boolean {
    if (
        channel.groupMatches("entertainment", "variety", "talk", "variet", "show", "comedy", "reality", "drama")
    ) return true
    return channel.name.contains("娱乐", ignoreCase = true) ||
        channel.name.contains("show", ignoreCase = true) ||
        channel.name.contains("电视剧", ignoreCase = true) ||
        channel.name.contains("tv show", ignoreCase = true) ||
        channel.name.contains("综艺", ignoreCase = true) ||
        channel.name.contains("大亨", ignoreCase = true)
}

private fun isMovie(channel: TvChannel): Boolean {
    if (channel.groupMatches("movie", "movies", "cinema", "film")) return true
    return channel.name.contains("电影", ignoreCase = true) ||
        channel.name.contains("movie", ignoreCase = true) ||
        channel.name.contains("cinema", ignoreCase = true) ||
        channel.name.contains("电影城", ignoreCase = true)
}

private fun isKids(channel: TvChannel): Boolean {
    if (channel.groupMatches("kids", "children", "children's", "animation", "family")) return true
    return channel.name.contains("少儿", ignoreCase = true) ||
        channel.name.contains("儿童", ignoreCase = true) ||
        channel.name.contains("kid", ignoreCase = true) ||
        channel.name.contains("animation", ignoreCase = true)
}

private fun isMusic(channel: TvChannel): Boolean {
    if (channel.groupMatches("music", "radio")) return true
    return channel.name.contains("音乐", ignoreCase = true) ||
        channel.name.contains("music", ignoreCase = true) ||
        channel.name.contains("radio", ignoreCase = true) ||
        channel.name.contains("fm", ignoreCase = true)
}

private fun isLifestyle(channel: TvChannel): Boolean {
    if (channel.groupMatches("lifestyle", "travel", "culture", "food", "cooking", "shop", "shopping", "fashion")) return true
    return channel.name.contains("生活", ignoreCase = true) ||
        channel.name.contains("travel", ignoreCase = true) ||
        channel.name.contains("时尚", ignoreCase = true) ||
        channel.name.contains("购物", ignoreCase = true) ||
        channel.name.contains("旅游", ignoreCase = true)
}

private fun isDocumentary(channel: TvChannel): Boolean {
    if (channel.groupMatches("documentary", "documentaries", "nature", "history", "discover")) return true
    return channel.name.contains("纪录", ignoreCase = true) ||
        channel.name.contains("document", ignoreCase = true) ||
        channel.name.contains("自然", ignoreCase = true) ||
        channel.name.contains("探險", ignoreCase = true) ||
        channel.name.contains("explore", ignoreCase = true)
}

private fun TvChannel.isInternational(): Boolean {
    if (isCctv(this) || isSatellite(this) || isLocal(this) || isNews(this) || isSports(this)) {
        return false
    }
    val hasChineseChars = hasChineseChars("${name} ${groupTitle}")
    val hasLatinChars = hasLatinLetters("${name} ${groupTitle}")
    val hasCountrySuffix = looksLikeCountrySuffix(id)
    val hasForeignHintByGroup = groupMatches(
        "international",
        "global",
        "world",
        "country",
        "network",
    )
    return hasCountrySuffix || hasForeignHintByGroup || (!hasChineseChars && hasLatinChars)
}

private fun TvChannel.channelSectionBucket(): String {
    return when {
        isCctv(this) -> "cctv"
        isSatellite(this) -> "satellite"
        isNews(this) -> "news"
        isSports(this) -> "sports"
        isMovie(this) -> "movie"
        isEntertainment(this) -> "entertainment"
        isMusic(this) -> "music"
        isLifestyle(this) -> "lifestyle"
        isKids(this) -> "kids"
        isDocumentary(this) -> "documentary"
        isLocal(this) -> "local"
        isInternational() -> "international"
        else -> "other"
    }
}

private val palette = listOf(
    Color(0xFF0057A8),
    Color(0xFF7C3AED),
    Color(0xFF0F766E),
    Color(0xFFB45309),
    Color(0xFF9333EA),
    Color(0xFF2563EB),
)

private fun TvChannel.groupTokens(): Set<String> {
    val raw = groupTitle.lowercase(Locale.ROOT)
    if (raw.isBlank()) return emptySet()
    return raw
        .split(';', ',', '|', '/', '，')
        .flatMap { token ->
            token
                .split(' ')
                .mapNotNull { piece ->
                    val normalized = piece.trim().lowercase(Locale.ROOT)
                    normalized.takeIf { it.isNotBlank() }
                }
        }
        .toSet()
}

private fun TvChannel.groupMatches(vararg keys: String): Boolean {
    val group = groupTokens()
    return keys.any { key ->
        val normalizedKey = key.lowercase(Locale.ROOT)
        group.any { token -> token == normalizedKey || token.startsWith("${normalizedKey}_") || token.startsWith("${normalizedKey}-") || token.contains(normalizedKey) }
    }
}

private fun hasChineseChars(value: String): Boolean = value.any { it.code in 0x4E00..0x9FFF }

private fun hasLatinLetters(value: String): Boolean = value.any { it.isLetter() && it.code <= 127 }

private fun looksLikeCountrySuffix(value: String): Boolean {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .substringAfterLast('.', "")
        .also { suffix ->
            if (suffix.length !in 2..3) return false
        }
        .let { suffix ->
                suffix.matches(Regex("[a-z]{2,3}")) &&
                suffix !in setOf("hd", "tv", "sd", "go", "net", "com", "cn", "cnm")
        }
}

private fun resolveCountryFromId(channelId: String): String? {
    val idSuffix = channelId.substringAfterLast('.', "").lowercase(Locale.ROOT).trim('.')
    normalizeCountryCandidate(idSuffix)?.let { return it }
    val compact = channelId
        .replace("-", ".")
        .replace("_", ".")
        .split('.')
        .map { it.trim() }
        .lastOrNull { token -> normalizeCountryCandidate(token) != null }
        ?: return null
    return normalizeCountryCandidate(compact)
}

private fun resolveCountryFromText(value: String): String? {
    val normalized = value.lowercase(Locale.ROOT)
    internationalCountryNameHints.forEach { (hint, country) ->
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
    internationalCountryNameHints[compact]?.let { return it }
    if (compact.length == 2 && isCountryCodeCandidate(compact)) return resolveCountryDisplayName(compact)
    if (compact.length == 3 && isCountryCodeCandidate(compact)) return resolveCountryDisplayName(compact)
    return null
}

private fun isCountryCodeCandidate(value: String): Boolean {
    val normalized = value.lowercase(Locale.ROOT)
    return normalized.length in 2..3 &&
        normalized.all { it.isLetter() } &&
        normalized.uppercase(Locale.ROOT) in isoCountryCodeLookup
}

private fun resolveCountryDisplayName(code: String): String {
    val country = Locale("", code.uppercase(Locale.ROOT)).getDisplayCountry(Locale.SIMPLIFIED_CHINESE)
    return if (country.isBlank()) code.uppercase(Locale.ROOT) else country
}

private fun localizedRegionHints(): List<String> = listOf(
    "北京",
    "上海",
    "天津",
    "重庆",
    "黑龙江",
    "辽宁",
    "吉林",
    "安徽",
    "江苏",
    "浙江",
    "福建",
    "江西",
    "山东",
    "河南",
    "湖北",
    "湖南",
    "广东",
    "广西",
    "海南",
    "四川",
    "贵州",
    "云南",
    "甘肃",
    "陕西",
    "山西",
    "贵州",
    "青海",
    "台湾",
    "香港",
    "澳门",
    "内蒙古",
    "新疆",
    "西藏",
    "宁夏",
)

private val bracketPattern = Regex("""\(([^)]+)\)""")
private val squarePattern = Regex("""\[([^\]]+)\]""")
private val codeLikePattern = Regex("""\b([A-Za-z]{2,3})\b""")
