package com.github.shelgen.timesage.domain

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Localization
import com.github.shelgen.timesage.configuration.PeriodicPlanning
import com.github.shelgen.timesage.configuration.Reminders
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.time.DateRange
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
        private val dummyTenant = Tenant(DiscordServerId(0), DiscordTextChannelId(0))

        private fun configWith(rules: Map<DayOfWeek, LocalTime>) = Configuration(
            tenant = dummyTenant,
            localization = Localization(timeZone = TimeZone.getTimeZone("UTC"), startDayOfWeek = DayOfWeek.MONDAY),
            activities = emptyList(),
            timeSlotRules = rules,
            reminders = Reminders.DEFAULT,
            periodicPlanning = PeriodicPlanning.DEFAULT,
            sessionLimit = 2,
        )

        @Suppress("unused")
        val parameters = listOf(
            Parameters(
                description = "only mondays",
                rules = mapOf(DayOfWeek.MONDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-04T12:34:00Z", "2025-08-11T12:34:00Z")
            ),
            Parameters(
                description = "only tuesdays",
                rules = mapOf(DayOfWeek.TUESDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-05T12:34:00Z", "2025-08-12T12:34:00Z")
            ),
            Parameters(
                description = "only wednesdays",
                rules = mapOf(DayOfWeek.WEDNESDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-06T12:34:00Z", "2025-08-13T12:34:00Z")
            ),
            Parameters(
                description = "only thursdays",
                rules = mapOf(DayOfWeek.THURSDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-07T12:34:00Z", "2025-08-14T12:34:00Z")
            ),
            Parameters(
                description = "only fridays",
                rules = mapOf(DayOfWeek.FRIDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-08T12:34:00Z", "2025-08-15T12:34:00Z")
            ),
            Parameters(
                description = "only saturdays",
                rules = mapOf(DayOfWeek.SATURDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-09T12:34:00Z", "2025-08-16T12:34:00Z")
            ),
            Parameters(
                description = "only sundays",
                rules = mapOf(DayOfWeek.SUNDAY to time),
                fromDate = "2025-08-04",
                toDate = "2025-08-17",
                expectedInstants = listOf("2025-08-10T12:34:00Z", "2025-08-17T12:34:00Z")
            ),
            Parameters(
                description = "weekdays only",
                rules = mapOf(
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
                rules = mapOf(
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
                rules = mapOf(
                    DayOfWeek.MONDAY to time,
                    DayOfWeek.TUESDAY to time,
                    DayOfWeek.WEDNESDAY to time,
                    DayOfWeek.THURSDAY to time,
                    DayOfWeek.FRIDAY to time,
                    DayOfWeek.SATURDAY to time,
                    DayOfWeek.SUNDAY to time,
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
                rules = mapOf(
                    DayOfWeek.MONDAY to LocalTime.parse("18:00"),
                    DayOfWeek.WEDNESDAY to LocalTime.parse("20:00"),
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
        val dateRange = DateRange(LocalDate.parse(parameters.fromDate), LocalDate.parse(parameters.toDate))
        val timeSlots = configWith(parameters.rules).produceTimeSlots(dateRange)
        assertEquals(parameters.expectedInstants.map(Instant::parse), timeSlots)
    }

    data class Parameters(
        val description: String,
        val rules: Map<DayOfWeek, LocalTime>,
        val fromDate: String,
        val toDate: String,
        val expectedInstants: List<String>
    ) {
        override fun toString() = description
    }
}
