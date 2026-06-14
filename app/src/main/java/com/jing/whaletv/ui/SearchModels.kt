package com.jing.whaletv.ui

import com.jing.whaletv.data.model.TvChannel
import java.nio.charset.Charset
import java.util.Locale

val SearchKeyboardKeys: List<String> = (
    ('A'..'Z').map(Char::toString) + ('0'..'9').map(Char::toString)
)

fun searchChannels(query: String, channels: List<TvChannel>): List<TvChannel> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    val compactQuery = query.toSearchCompactKey()
    if (normalizedQuery.isBlank()) return emptyList()
    return channels
        .filter { channel ->
            val fields = channel.searchFields()
            fields.any { value -> value.lowercase(Locale.ROOT).contains(normalizedQuery) } ||
                compactQuery.isNotBlank() && fields.any { value ->
                    value.toSearchCompactKey().contains(compactQuery) ||
                        value.toTokenInitialKey().contains(compactQuery)
                }
        }
        .sortedWith(compareBy<TvChannel> { it.homeDesignRank() }.thenBy { it.priority }.thenBy { it.name })
}

private fun TvChannel.searchFields(): List<String> {
    val cardItem = toChannelCardItem()
    return listOf(
        name,
        id,
        groupTitle,
        cardItem.title,
        cardItem.categoryLabel,
        cardItem.logoLabel,
    )
}

private fun String.toSearchCompactKey(): String {
    return buildString {
        this@toSearchCompactKey.forEach { char ->
            when {
                char.isLetterOrDigit() && char.code < ASCII_LIMIT -> append(char.lowercaseChar())
                else -> char.toChinesePinyinInitial()?.let(::append)
            }
        }
    }
}

private fun String.toTokenInitialKey(): String {
    return split(NonAlphaNumericPattern)
        .filter { it.isNotBlank() }
        .joinToString(separator = "") { token ->
            token.firstOrNull()
                ?.takeIf { it.isLetterOrDigit() && it.code < ASCII_LIMIT }
                ?.lowercaseChar()
                ?.toString()
                .orEmpty()
        }
}

private fun Char.toChinesePinyinInitial(): Char? {
    val bytes = toString().toByteArray(GbkCharset)
    if (bytes.size < 2) return null
    val code = ((bytes[0].toInt() and 0xff) shl 8) + (bytes[1].toInt() and 0xff)
    return PinyinInitialRanges.firstOrNull { code in it.range }?.initial
}

private data class PinyinInitialRange(
    val range: IntRange,
    val initial: Char,
)

private const val ASCII_LIMIT = 128
private val NonAlphaNumericPattern = Regex("""[^A-Za-z0-9]+""")
private val GbkCharset: Charset = Charset.forName("GBK")
private val PinyinInitialRanges = listOf(
    PinyinInitialRange(45217..45252, 'a'),
    PinyinInitialRange(45253..45760, 'b'),
    PinyinInitialRange(45761..46317, 'c'),
    PinyinInitialRange(46318..46825, 'd'),
    PinyinInitialRange(46826..47009, 'e'),
    PinyinInitialRange(47010..47296, 'f'),
    PinyinInitialRange(47297..47613, 'g'),
    PinyinInitialRange(47614..48118, 'h'),
    PinyinInitialRange(48119..49061, 'j'),
    PinyinInitialRange(49062..49323, 'k'),
    PinyinInitialRange(49324..49895, 'l'),
    PinyinInitialRange(49896..50370, 'm'),
    PinyinInitialRange(50371..50613, 'n'),
    PinyinInitialRange(50614..50621, 'o'),
    PinyinInitialRange(50622..50905, 'p'),
    PinyinInitialRange(50906..51386, 'q'),
    PinyinInitialRange(51387..51445, 'r'),
    PinyinInitialRange(51446..52217, 's'),
    PinyinInitialRange(52218..52697, 't'),
    PinyinInitialRange(52698..52979, 'w'),
    PinyinInitialRange(52980..53688, 'x'),
    PinyinInitialRange(53689..54480, 'y'),
    PinyinInitialRange(54481..55289, 'z'),
)
