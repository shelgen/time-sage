package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TimeSlotRule(
    val dayType: DayType,
    val timeOfDayUtc: LocalTime
) {
    companion object {
        val DEFAULT = TimeSlotRule(
            dayType = DayType.EVERY_DAY,
            timeOfDayUtc = LocalTime.parse("18:30")
        )
    }

    fun getTimeSlots(datePeriod: DatePeriod): List<Instant> =
        datePeriod.dates()
            .filter(dayType::includes)
            .map { date -> date.atTime(timeOfDayUtc).atOffset(ZoneOffset.UTC) }
            .map(OffsetDateTime::toInstant)
}
