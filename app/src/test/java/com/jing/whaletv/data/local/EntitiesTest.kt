package com.jing.whaletv.data.local

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EntitiesTest {
    @Test
    fun toDomain_selectsLatestCurrentAndEarliestNextProgram() {
        val programs = listOf(
            program("Program A", 1_000L, 5_000L),
            program("Program B", 2_000L, 3_000L),
            program("Program C", 4_000L, 6_000L),
            program("Program D", 6_500L, 8_000L),
        )

        val now = Instant.parse("1970-01-01T00:00:04Z").toEpochMilli()
        val domain = ChannelWithStreams(
            channel = testChannel(),
            streams = emptyList(),
        ).toDomain(now, programs.shuffled())

        assertEquals("Program C", domain.currentProgram?.title)
        assertEquals("Program D", domain.nextProgram?.title)
        assertNotNull(domain.currentProgram)
        assertEquals(6_500L, domain.nextProgram?.startAt)
        assertEquals(listOf("Program C", "Program D"), domain.schedulePrograms.map { it.title })
    }

    @Test
    fun toDomain_returnsNullWhenNoCurrentOrNext() {
        val now = Instant.parse("1970-01-01T00:00:10Z").toEpochMilli()
        val programs = listOf(
            program("Program A", 1_000L, 2_000L),
        )

        val domain = ChannelWithStreams(
            channel = testChannel(),
            streams = emptyList(),
        ).toDomain(now, programs)

        assertNull(domain.currentProgram)
        assertNull(domain.nextProgram)
        assertEquals(emptyList<String>(), domain.schedulePrograms.map { it.title })
    }

    @Test
    fun toDomain_returnsSortedCurrentAndUpcomingProgramsWithLimit() {
        val now = Instant.parse("1970-01-01T00:00:10Z").toEpochMilli()
        val programs = listOf(
            program("Future 6", 16_000L, 17_000L),
            program("Future 1", 11_000L, 12_000L),
            program("Future 4", 14_000L, 15_000L),
            program("Current", 9_000L, 10_500L),
            program("Future 7", 17_000L, 18_000L),
            program("Future 2", 12_000L, 13_000L),
            program("Future 5", 15_000L, 16_000L),
            program("Future 3", 13_000L, 14_000L),
        )

        val domain = ChannelWithStreams(
            channel = testChannel(),
            streams = emptyList(),
        ).toDomain(now, programs)

        assertEquals(
            listOf("Current", "Future 1", "Future 2", "Future 3", "Future 4", "Future 5"),
            domain.schedulePrograms.map { it.title },
        )
        assertEquals(6, domain.schedulePrograms.size)
    }

    private fun testChannel(): ChannelEntity = ChannelEntity(
        id = "c1",
        name = "Test",
        logoUrl = null,
        groupTitle = "General",
        priority = 0,
        isFavorite = false,
        isAvailable = true,
        updatedAt = 0L,
    )

    private fun program(title: String, startAt: Long, endAt: Long): ProgramEntity = ProgramEntity(
        channelId = "c1",
        title = title,
        startAt = startAt,
        endAt = endAt,
        description = null,
    )
}
