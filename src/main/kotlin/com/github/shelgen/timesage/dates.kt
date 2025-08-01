package com.github.shelgen.timesage

import java.time.*

fun nextWeekStartDate(startDayOfWeek: DayOfWeek) =
    LocalDate.now(ZoneOffset.UTC).plusWeeks(1).getStartDateOfSameWeek(startDayOfWeek)
fun weekDatesStartingWith(startDate: LocalDate) = (0L..6L).map(startDate::plusDays)
fun LocalDate.getStartDateOfSameWeek(startDayOfWeek: DayOfWeek): LocalDate {
    val daysToSubtract = (DayOfWeek.entries.size + this.dayOfWeek.value - startDayOfWeek.value) % DayOfWeek.entries.size
    return minusDays(daysToSubtract.toLong())
}
fun LocalDate.atNormalStartTime(): OffsetDateTime = atTime(LocalTime.of(18, 30)).atOffset(ZoneOffset.UTC)
