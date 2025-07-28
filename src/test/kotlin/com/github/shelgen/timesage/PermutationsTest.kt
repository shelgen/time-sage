package com.github.shelgen.timesage

import com.github.shelgen.timesage.planning.Planner.Companion.permutations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PermutationsTest {
    @Test
    fun test() {
        val options = setOf(1L, 2L, 3L)

        val permutations = options.permutations()
        assertTrue(setOf(1L) in permutations)
        assertTrue(setOf(1L, 2L) in permutations)
        assertTrue(setOf(1L, 2L, 3L) in permutations)
        assertTrue(setOf(1L, 3L) in permutations)
        assertTrue(setOf(2L) in permutations)
        assertTrue(setOf(2L, 3L) in permutations)
        assertTrue(setOf(3L) in permutations)
        assertEquals(7, permutations.size)
    }
}
