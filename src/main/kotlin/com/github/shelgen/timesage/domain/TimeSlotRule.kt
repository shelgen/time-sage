package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*

class TimeSlotRule(
    val dayType: DayType,
    val timeOfDay: LocalTime
) {
    companion object {
        val DEFAULT = TimeSlotRule(
            dayType = DayType.EVERY_DAY,
            timeOfDay = LocalTime.parse("18:00")
        )
    }

    fun getTimeSlots(datePeriod: DatePeriod, timeZone: TimeZone): List<Instant> =
        datePeriod.dates()
            .filter(dayType::includes)
            .map { date -> date.atTime(timeOfDay).atZone(timeZone.toZoneId()) }
            .map(ZonedDateTime::toInstant)
}
