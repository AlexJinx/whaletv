package com.jing.whaletv.ui

import com.jing.whaletv.ui.screens.ProgramListScrollDirection
import com.jing.whaletv.ui.screens.coercePlaybackStreamIndex
import com.jing.whaletv.ui.screens.programListScrollTargetIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerProgramListTest {
    @Test
    fun programListScrollTargetIndex_movesWithinBounds() {
        assertEquals(
            1,
            programListScrollTargetIndex(
                currentIndex = 0,
                itemCount = 3,
                direction = ProgramListScrollDirection.DOWN,
            ),
        )
        assertEquals(
            1,
            programListScrollTargetIndex(
                currentIndex = 2,
                itemCount = 3,
                direction = ProgramListScrollDirection.UP,
            ),
        )
    }

    @Test
    fun programListScrollTargetIndex_staysAtEdges() {
        assertEquals(
            0,
            programListScrollTargetIndex(
                currentIndex = 0,
                itemCount = 3,
                direction = ProgramListScrollDirection.UP,
            ),
        )
        assertEquals(
            2,
            programListScrollTargetIndex(
                currentIndex = 2,
                itemCount = 3,
                direction = ProgramListScrollDirection.DOWN,
            ),
        )
    }

    @Test
    fun programListScrollTargetIndex_handlesEmptyAndOutOfRangeInput() {
        assertEquals(
            0,
            programListScrollTargetIndex(
                currentIndex = 5,
                itemCount = 0,
                direction = ProgramListScrollDirection.DOWN,
            ),
        )
        assertEquals(
            1,
            programListScrollTargetIndex(
                currentIndex = -4,
                itemCount = 3,
                direction = ProgramListScrollDirection.DOWN,
            ),
        )
    }

    @Test
    fun coercePlaybackStreamIndex_clampsToAvailableStreams() {
        assertEquals(0, coercePlaybackStreamIndex(currentIndex = -1, streamCount = 3))
        assertEquals(1, coercePlaybackStreamIndex(currentIndex = 1, streamCount = 3))
        assertEquals(2, coercePlaybackStreamIndex(currentIndex = 5, streamCount = 3))
    }

    @Test
    fun coercePlaybackStreamIndex_returnsZeroWhenNoStreamsRemain() {
        assertEquals(0, coercePlaybackStreamIndex(currentIndex = 2, streamCount = 0))
        assertEquals(0, coercePlaybackStreamIndex(currentIndex = 2, streamCount = -1))
    }
}
