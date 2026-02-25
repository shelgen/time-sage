package com.github.shelgen.timesage

import com.github.shelgen.timesage.planning.Planner.Companion.combinations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombinationsTest {
    @Test
    fun test() {
        val options = setOf(1L, 2L, 3L)

        val combinations = options.combinations()
        assertTrue(emptySet() in combinations)
        assertTrue(setOf(1L) in combinations)
        assertTrue(setOf(1L, 2L) in combinations)
        assertTrue(setOf(1L, 2L, 3L) in combinations)
        assertTrue(setOf(1L, 3L) in combinations)
        assertTrue(setOf(2L) in combinations)
        assertTrue(setOf(2L, 3L) in combinations)
        assertTrue(setOf(3L) in combinations)
        assertEquals(8, combinations.size)
    }

    @Test
    fun `empty permutation is added`() {
        val options = emptySet<Int>()

        val combinations = options.combinations()
        assertTrue(emptySet() in combinations)
        assertEquals(1, combinations.size)
    }
}
