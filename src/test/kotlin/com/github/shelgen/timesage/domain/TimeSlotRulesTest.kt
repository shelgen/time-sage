package com.github.shelgen.timesage.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class TimeSlotRulesTest {
    companion object {
        private val time = LocalTime.parse("12:34")

        @Suppress("unused")
        val parameters = listOf(
            Parameters(
                description = "only mondays",
                rules = TimeSlotRules.of(DayOfWeek.MONDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-04T12:34:00Z", "2025-08-11T12:34:00Z")
            ),
            Parameters(
                description = "only tuesdays",
                rules = TimeSlotRules.of(DayOfWeek.TUESDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-05T12:34:00Z", "2025-08-12T12:34:00Z")
            ),
            Parameters(
                description = "only wednesdays",
                rules = TimeSlotRules.of(DayOfWeek.WEDNESDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-06T12:34:00Z", "2025-08-13T12:34:00Z")
            ),
            Parameters(
                description = "only thursdays",
                rules = TimeSlotRules.of(DayOfWeek.THURSDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-07T12:34:00Z", "2025-08-14T12:34:00Z")
            ),
            Parameters(
                description = "only fridays",
                rules = TimeSlotRules.of(DayOfWeek.FRIDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-08T12:34:00Z", "2025-08-15T12:34:00Z")
            ),
            Parameters(
                description = "only saturdays",
                rules = TimeSlotRules.of(DayOfWeek.SATURDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-09T12:34:00Z", "2025-08-16T12:34:00Z")
            ),
            Parameters(
                description = "only sundays",
                rules = TimeSlotRules.of(DayOfWeek.SUNDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-10T12:34:00Z", "2025-08-17T12:34:00Z")
            ),
            Parameters(
                description = "weekdays only",
                rules = TimeSlotRules.of(
                    DayOfWeek.MONDAY to time,
                    DayOfWeek.TUESDAY to time,
                    DayOfWeek.WEDNESDAY to time,
                    DayOfWeek.THURSDAY to time,
                    DayOfWeek.FRIDAY to time,
                ),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf(
                    "2025-08-04T12:34:00Z",
                    "2025-08-05T12:34:00Z",
                    "2025-08-06T12:34:00Z",
                    "2025-08-07T12:34:00Z",
                    "2025-08-08T12:34:00Z",
                    "2025-08-11T12:34:00Z",
                    "2025-08-12T12:34:00Z",
                    "2025-08-13T12:34:00Z",
                    "2025-08-14T12:34:00Z",
                    "2025-08-15T12:34:00Z"
                )
            ),
            Parameters(
                description = "weekends only",
                rules = TimeSlotRules.of(
                    DayOfWeek.SATURDAY to time,
                    DayOfWeek.SUNDAY to time,
                ),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf(
                    "2025-08-09T12:34:00Z",
                    "2025-08-10T12:34:00Z",
                    "2025-08-16T12:34:00Z",
                    "2025-08-17T12:34:00Z"
                )
            ),
            Parameters(
                description = "every day",
                rules = TimeSlotRules(
                    mondays = time,
                    tuesdays = time,
                    wednesdays = time,
                    thursdays = time,
                    fridays = time,
                    saturdays = time,
                    sundays = time,
                ),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf(
                    "2025-08-04T12:34:00Z",
                    "2025-08-05T12:34:00Z",
                    "2025-08-06T12:34:00Z",
                    "2025-08-07T12:34:00Z",
                    "2025-08-08T12:34:00Z",
                    "2025-08-09T12:34:00Z",
                    "2025-08-10T12:34:00Z",
                    "2025-08-11T12:34:00Z",
                    "2025-08-12T12:34:00Z",
                    "2025-08-13T12:34:00Z",
                    "2025-08-14T12:34:00Z",
                    "2025-08-15T12:34:00Z",
                    "2025-08-16T12:34:00Z",
                    "2025-08-17T12:34:00Z"
                )
            ),
            Parameters(
                description = "mixed times per day",
                rules = TimeSlotRules(
                    mondays = LocalTime.parse("18:00"),
                    tuesdays = null,
                    wednesdays = LocalTime.parse("20:00"),
                    thursdays = null,
                    fridays = null,
                    saturdays = null,
                    sundays = null,
                ),
                fromDate = "2025-08-04",
                toDate = "2025-08-10",
                expectedInstants = listOf("2025-08-04T18:00:00Z", "2025-08-06T20:00:00Z")
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("parameters")
    fun `returns correct time slots`(parameters: Parameters) {
        // Given:
        val dateRange = DateRange(LocalDate.parse(parameters.fromDate), LocalDate.parse(parameters.toDate))

        // When:
        val timeSlots = parameters.rules.getTimeSlots(dateRange, TimeZone.getTimeZone("UTC"))

        // Then:
        assertEquals(parameters.expectedInstants.map(Instant::parse), timeSlots)
    }

    data class Parameters(
        val description: String,
        val rules: TimeSlotRules,
        val fromDate: String,
        val toDate: String,
        val expectedInstants: List<String>
    ) {
        override fun toString() = description
    }
}
