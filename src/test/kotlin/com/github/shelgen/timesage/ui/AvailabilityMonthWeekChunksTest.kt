package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.domain.DateRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class AvailabilityMonthWeekChunksTest {

    @Test
    fun `march 2026 with thursday start - example from spec`() {
        val chunks = DateRange.from(YearMonth.of(2026, 3)).chunkedByWeek(DayOfWeek.THURSDAY)

        assertEquals(5, chunks.size)
        assertEquals(DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4)), chunks[0])
        assertEquals(DateRange(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 11)), chunks[1])
        assertEquals(DateRange(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 18)), chunks[2])
        assertEquals(DateRange(LocalDate.of(2026, 3, 19), LocalDate.of(2026, 3, 25)), chunks[3])
        assertEquals(DateRange(LocalDate.of(2026, 3, 26), LocalDate.of(2026, 3, 31)), chunks[4])
    }

    @Test
    fun `month starting exactly on start day produces no partial first week`() {
        // March 2021 starts on a Monday
        val chunks = DateRange.from(YearMonth.of(2021, 3)).chunkedByWeek(DayOfWeek.MONDAY)

        assertEquals(5, chunks.size)
        assertEquals(DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 3, 7)), chunks[0])
        assertEquals(DateRange(LocalDate.of(2021, 3, 8), LocalDate.of(2021, 3, 14)), chunks[1])
    }

    @Test
    fun `february 2026 with monday start`() {
        // Feb 2026: starts on Sunday, 28 days
        val chunks = DateRange.from(YearMonth.of(2026, 2)).chunkedByWeek(DayOfWeek.MONDAY)

        // First chunk: Feb 1 (Sunday only, before the first Monday Feb 2)
        assertEquals(DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1)), chunks[0])
        // Second: Feb 2–8
        assertEquals(DateRange(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 8)), chunks[1])
        // Last: Feb 23–28
        assertEquals(DateRange(LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 28)), chunks.last())
    }
}
