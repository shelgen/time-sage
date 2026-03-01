package com.github.shelgen.timesage

import java.time.*
import java.util.TimeZone

fun plannedWeekStartDate(startDayOfWeek: DayOfWeek, timeZone: TimeZone, daysBeforePeriod: Int): LocalDate =
    LocalDate.now(timeZone.toZoneId()).plusDays(daysBeforePeriod.toLong()).getStartDateOfSameWeek(startDayOfWeek)

fun plannedYearMonth(timeZone: TimeZone, daysBeforePeriod: Int): YearMonth =
    YearMonth.from(LocalDate.now(timeZone.toZoneId()).plusDays(daysBeforePeriod.toLong()))

fun weekDatesStartingWith(startDate: LocalDate) = (0L..6L).map(startDate::plusDays)
fun LocalDate.getStartDateOfSameWeek(startDayOfWeek: DayOfWeek): LocalDate {
    val daysToSubtract = (DayOfWeek.entries.size + this.dayOfWeek.value - startDayOfWeek.value) % DayOfWeek.entries.size
    return minusDays(daysToSubtract.toLong())
}
