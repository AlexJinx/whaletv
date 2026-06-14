package com.jing.whaletv.ui

import com.jing.whaletv.data.model.TvChannel
import java.util.Locale

val SearchKeyboardKeys: List<String> = (
    ('A'..'Z').map(Char::toString) + ('0'..'9').map(Char::toString)
)

fun searchChannels(query: String, channels: List<TvChannel>): List<TvChannel> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    if (normalizedQuery.isBlank()) return emptyList()
    return channels
        .filter { channel ->
            listOf(channel.name, channel.id, channel.groupTitle)
                .any { value -> value.lowercase(Locale.ROOT).contains(normalizedQuery) }
        }
        .sortedWith(compareBy<TvChannel> { it.homeDesignRank() }.thenBy { it.priority }.thenBy { it.name })
}
