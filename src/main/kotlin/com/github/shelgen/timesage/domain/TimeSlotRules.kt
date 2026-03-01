package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.*

data class TimeSlotRules(
    val mondays: LocalTime?,
    val tuesdays: LocalTime?,
    val wednesdays: LocalTime?,
    val thursdays: LocalTime?,
    val fridays: LocalTime?,
    val saturdays: LocalTime?,
    val sundays: LocalTime?,
) {
    companion object {
        val DEFAULT = fromEveryDay(LocalTime.parse("18:00"))

        fun fromEveryDay(timeOfDay: LocalTime) = TimeSlotRules(
            mondays = timeOfDay,
            tuesdays = timeOfDay,
            wednesdays = timeOfDay,
            thursdays = timeOfDay,
            fridays = timeOfDay,
            saturdays = timeOfDay,
            sundays = timeOfDay,
        )

        fun of(vararg entries: Pair<DayOfWeek, LocalTime>) = TimeSlotRules(
            mondays = entries.firstOrNull { it.first == DayOfWeek.MONDAY }?.second,
            tuesdays = entries.firstOrNull { it.first == DayOfWeek.TUESDAY }?.second,
            wednesdays = entries.firstOrNull { it.first == DayOfWeek.WEDNESDAY }?.second,
            thursdays = entries.firstOrNull { it.first == DayOfWeek.THURSDAY }?.second,
            fridays = entries.firstOrNull { it.first == DayOfWeek.FRIDAY }?.second,
            saturdays = entries.firstOrNull { it.first == DayOfWeek.SATURDAY }?.second,
            sundays = entries.firstOrNull { it.first == DayOfWeek.SUNDAY }?.second,
        )
    }

    operator fun get(dayOfWeek: DayOfWeek): LocalTime? = when (dayOfWeek) {
        DayOfWeek.MONDAY -> mondays
        DayOfWeek.TUESDAY -> tuesdays
        DayOfWeek.WEDNESDAY -> wednesdays
        DayOfWeek.THURSDAY -> thursdays
        DayOfWeek.FRIDAY -> fridays
        DayOfWeek.SATURDAY -> saturdays
        DayOfWeek.SUNDAY -> sundays
    }

    fun getTimeSlots(datePeriod: DatePeriod, timeZone: TimeZone): List<Instant> =
        datePeriod.dates().mapNotNull { date ->
            this[date.dayOfWeek]?.let { time ->
                date.atTime(time).atZone(timeZone.toZoneId()).toInstant()
            }
        }
}
