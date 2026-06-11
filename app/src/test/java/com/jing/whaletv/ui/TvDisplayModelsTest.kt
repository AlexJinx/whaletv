package com.jing.whaletv.ui

import com.jing.whaletv.data.model.TvChannel
import com.jing.whaletv.data.model.StreamHealth
import com.jing.whaletv.data.model.TvStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvDisplayModelsTest {
    @Test
    fun sectionsContainFavoriteKeyAndChannelFilterMatchesFavoriteState() {
        val channels = listOf(
            TvChannel(
                id = "1",
                name = "A",
                logoUrl = null,
                groupTitle = "General",
                priority = 0,
                isFavorite = true,
                lastWatchedAt = null,
                isAvailable = true,
                streams = listOf(
                    TvStream(
                        channelId = "1",
                        url = "http://example.com/a",
                        quality = null,
                        label = null,
                        referrer = null,
                        userAgent = null,
                        healthStatus = StreamHealth.UNKNOWN,
                        failureCount = 0,
                        lastFailureAt = null,
                        lastSuccessAt = null,
                        sortOrder = 0,
                    ),
                ),
                currentProgram = null,
                nextProgram = null,
            ),
            TvChannel(
                id = "2",
                name = "B",
                logoUrl = null,
                groupTitle = "General",
                priority = 1,
                isFavorite = false,
                lastWatchedAt = null,
                isAvailable = true,
                streams = listOf(
                    TvStream(
                        channelId = "2",
                        url = "http://example.com/b",
                        quality = null,
                        label = null,
                        referrer = null,
                        userAgent = null,
                        healthStatus = StreamHealth.UNHEALTHY,
                        failureCount = 0,
                        lastFailureAt = null,
                        lastSuccessAt = null,
                        sortOrder = 0,
                    ),
                ),
                currentProgram = null,
                nextProgram = null,
            ),
        )

        assertTrue(TvNavSections.any { it.id == "favorite" })
        assertEquals(1, channelsForSection("favorite", channels).size)
    }
}
