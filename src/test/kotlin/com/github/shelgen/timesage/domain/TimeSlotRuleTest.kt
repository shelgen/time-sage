package com.github.shelgen.timesage.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class TimeSlotRuleTest {
    companion object {
        @Suppress("unused")
        val parameters = listOf(
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.MONDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-04T12:34:00Z", "2025-08-11T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.TUESDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-05T12:34:00Z", "2025-08-12T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.WEDNESDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-06T12:34:00Z", "2025-08-13T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.THURSDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-07T12:34:00Z", "2025-08-14T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.FRIDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-08T12:34:00Z", "2025-08-15T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.SATURDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-09T12:34:00Z", "2025-08-16T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.SUNDAYS,
                timeUtc = "12:34",
                expectedInstants = listOf("2025-08-10T12:34:00Z", "2025-08-17T12:34:00Z")
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.WEEKDAYS,
                timeUtc = "12:34",
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
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.WEEKENDS,
                timeUtc = "12:34",
                expectedInstants = listOf(
                    "2025-08-09T12:34:00Z",
                    "2025-08-10T12:34:00Z",
                    "2025-08-16T12:34:00Z",
                    "2025-08-17T12:34:00Z"
                )
            ),
            Parameters(
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                dayType = DayType.EVERY_DAY,
                timeUtc = "12:34",
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
        )
    }

    @ParameterizedTest
    @FieldSource("parameters")
    fun `returns correct time slots`(parameters: Parameters) {
        // Given:
        val datePeriod = DatePeriod(LocalDate.parse(parameters.fromDate), LocalDate.parse(parameters.toDate))
        val rule = TimeSlotRule(parameters.dayType, LocalTime.parse(parameters.timeUtc))

        // When:
        val timeSlots = rule.getTimeSlots(datePeriod, TimeZone.getTimeZone("UTC"))

        // Then:
        assertEquals(parameters.expectedInstants.map(Instant::parse), timeSlots)
    }

    data class Parameters(
        val fromDate: String,
        val toDate: String,
        val dayType: DayType,
        val timeUtc: String,
        val expectedInstants: List<String>
    )
}
