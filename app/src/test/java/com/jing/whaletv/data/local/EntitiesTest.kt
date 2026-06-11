package com.jing.whaletv.data.local

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EntitiesTest {
    @Test
    fun toDomain_selectsLatestCurrentAndEarliestNextProgram() {
        val channel = ChannelEntity(
            id = "c1",
            name = "Test",
            logoUrl = null,
            groupTitle = "General",
            priority = 0,
            isFavorite = false,
            isAvailable = true,
            updatedAt = 0L,
        )
        val programs = listOf(
            ProgramEntity(channelId = "c1", title = "Program A", startAt = 1_000L, endAt = 5_000L, description = null),
            ProgramEntity(channelId = "c1", title = "Program B", startAt = 2_000L, endAt = 3_000L, description = null),
            ProgramEntity(channelId = "c1", title = "Program C", startAt = 4_000L, endAt = 6_000L, description = null),
            ProgramEntity(channelId = "c1", title = "Program D", startAt = 6_500L, endAt = 8_000L, description = null),
        )

        val now = Instant.parse("1970-01-01T00:00:04Z").toEpochMilli()
        val domain = ChannelWithStreams(
            channel = channel,
            streams = emptyList(),
        ).toDomain(now, programs.shuffled())

        assertEquals("Program C", domain.currentProgram?.title)
        assertEquals("Program D", domain.nextProgram?.title)
        assertNotNull(domain.currentProgram)
        assertEquals(6_500L, domain.nextProgram?.startAt)
    }

    @Test
    fun toDomain_returnsNullWhenNoCurrentOrNext() {
        val channel = ChannelEntity(
            id = "c1",
            name = "Test",
            logoUrl = null,
            groupTitle = "General",
            priority = 0,
            isFavorite = false,
            isAvailable = true,
            updatedAt = 0L,
        )
        val now = Instant.parse("1970-01-01T00:00:10Z").toEpochMilli()
        val programs = listOf(
            ProgramEntity(channelId = "c1", title = "Program A", startAt = 1_000L, endAt = 2_000L, description = null),
        )

        val domain = ChannelWithStreams(
            channel = channel,
            streams = emptyList(),
        ).toDomain(now, programs)

        assertNull(domain.currentProgram)
        assertNull(domain.nextProgram)
    }
}
