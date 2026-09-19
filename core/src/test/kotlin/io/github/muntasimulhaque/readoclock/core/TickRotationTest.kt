package io.github.muntasimulhaque.readoclock.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TickRotationTest {

    @Test
    fun cyclesThroughEveryVariantBeforeRepeating() {
        val rotation = TickRotation(4)
        for (i in 0 until 4) rotation.loaded(i)
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2, 3), (0 until 8).map { rotation.take() })
    }

    @Test
    fun noPieceDecodedMeansNoTick() {
        val rotation = TickRotation(4)
        assertNull("nothing to play yet", rotation.take())
        assertNull(rotation.take())
        assertEquals(0, rotation.readyCount())
    }

    @Test
    fun aPieceThatNeverDecodesIsSkippedNotWaitedFor() {
        val rotation = TickRotation(4)
        rotation.loaded(0)
        rotation.loaded(1)
        // Variants 2 and 3 fail to decode. The cycle must still sound every
        // step, repeating 0 and 1 rather than pausing on the dead slots.
        assertEquals(listOf(0, 1, 0, 1, 0, 1), (0 until 6).map { rotation.take() })
    }

    @Test
    fun aNeighbourRepeatsOnlyWhenThereIsNoOtherChoice() {
        val rotation = TickRotation(3)
        rotation.loaded(0)
        rotation.loaded(2)
        // 1 never decodes, so 0 and 2 alternate; neither is ever doubled back
        // on while the other is available.
        assertEquals(listOf(0, 2, 0, 2, 0, 2), (0 until 6).map { rotation.take() })
    }

    @Test
    fun loadingOneAfterTheFactJoinsTheCycle() {
        val rotation = TickRotation(4)
        rotation.loaded(0)
        assertEquals(0, rotation.take())
        rotation.loaded(1)
        assertEquals(1, rotation.take())
        rotation.loaded(2)
        assertEquals(2, rotation.take())
    }

    @Test
    fun soundingIsRememberedSoTheRetryCanBeArmed() {
        val rotation = TickRotation(2)
        assertNull("nothing has decoded yet", rotation.take())
        rotation.loaded(1)
        assertEquals("the piece that arrives is the one that sounds", 1, rotation.take())
        assertEquals("and it repeats until a neighbour arrives", 1, rotation.take())
    }

    @Test
    fun onePieceIsAValidClock() {
        val rotation = TickRotation(1)
        rotation.loaded(0)
        assertEquals(listOf(0, 0, 0), (0 until 3).map { rotation.take() })
    }
}
