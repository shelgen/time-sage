package com.github.shelgen.timesage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import java.time.DayOfWeek
import java.time.LocalDate


class DatesTests {
    companion object {
        val parameters = listOf(
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.MONDAY,
                expectedDate = "2025-08-04"
            ),
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.TUESDAY,
                expectedDate = "2025-08-05"
            ),
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.WEDNESDAY,
                expectedDate = "2025-08-06"
            ),
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.THURSDAY,
                expectedDate = "2025-08-07"
            ),
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.FRIDAY,
                expectedDate = "2025-08-08"
            ),
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.SATURDAY,
                expectedDate = "2025-08-09"
            ),
            Parameters(
                date = "2025-08-09", // A Saturday
                startDayOfWeek = DayOfWeek.SUNDAY,
                expectedDate = "2025-08-03"
            ),
            Parameters(
                date = "2025-08-05", // A Tuesday
                startDayOfWeek = DayOfWeek.THURSDAY,
                expectedDate = "2025-07-31"
            )
        )
    }

    @ParameterizedTest
    @FieldSource("parameters")
    fun `returns correct start date of week`(parameters: Parameters) {
        // When:
        val startDateOfSameWeek = LocalDate.parse(parameters.date).getStartDateOfSameWeek(parameters.startDayOfWeek)

        // Then:
        assertEquals(parameters.startDayOfWeek, startDateOfSameWeek.dayOfWeek)
        assertEquals(LocalDate.parse(parameters.expectedDate), startDateOfSameWeek)
    }

    private fun LocalDate.getStartDateOfSameWeek(startDayOfWeek: DayOfWeek): LocalDate {
        val daysToSubtract = (DayOfWeek.entries.size + this.dayOfWeek.value - startDayOfWeek.value) % DayOfWeek.entries.size
        return minusDays(daysToSubtract.toLong())
    }

    data class Parameters(
        val date: String,
        val startDayOfWeek: DayOfWeek,
        val expectedDate: String
    )
}
