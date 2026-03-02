package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.ui.screens.AvailabilityScreen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class AvailabilityMonthWeekChunksTest {

    @Test
    fun `march 2026 with thursday start - example from spec`() {
        val chunks = AvailabilityScreen.weekChunks(DatePeriod.monthFrom(YearMonth.of(2026, 3)), DayOfWeek.THURSDAY)

        assertEquals(5, chunks.size)
        assertEquals(listOf(1, 2, 3, 4).map { LocalDate.of(2026, 3, it) }, chunks[0])
        assertEquals((5..11).map { LocalDate.of(2026, 3, it) }, chunks[1])
        assertEquals((12..18).map { LocalDate.of(2026, 3, it) }, chunks[2])
        assertEquals((19..25).map { LocalDate.of(2026, 3, it) }, chunks[3])
        assertEquals((26..31).map { LocalDate.of(2026, 3, it) }, chunks[4])
    }

    @Test
    fun `month starting exactly on start day produces no partial first week`() {
        // March 2021 starts on a Monday
        val chunks = AvailabilityScreen.weekChunks(DatePeriod.monthFrom(YearMonth.of(2021, 3)), DayOfWeek.MONDAY)

        assertEquals(5, chunks.size)
        assertEquals((1..7).map { LocalDate.of(2021, 3, it) }, chunks[0])
        assertEquals((8..14).map { LocalDate.of(2021, 3, it) }, chunks[1])
    }

    @Test
    fun `february 2026 with monday start`() {
        // Feb 2026: starts on Sunday, 28 days
        val chunks = AvailabilityScreen.weekChunks(DatePeriod.monthFrom(YearMonth.of(2026, 2)), DayOfWeek.MONDAY)

        // First chunk: Feb 1 (Sunday only, before the first Monday Feb 2)
        assertEquals(listOf(LocalDate.of(2026, 2, 1)), chunks[0])
        // Second: Feb 2–8
        assertEquals((2..8).map { LocalDate.of(2026, 2, it) }, chunks[1])
        // Last: Feb 23–28
        assertEquals((23..28).map { LocalDate.of(2026, 2, it) }, chunks.last())
    }
}
