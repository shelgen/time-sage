package com.github.shelgen.timesage

import java.time.*

fun nextMonday() = ZonedDateTime.now(ZoneOffset.UTC).plusDays(5).toLocalDate().mondayOfSameWeek()
fun weekDatesForMonday(weekMondayDate: LocalDate) = (0L..6L).map(weekMondayDate::plusDays)
fun LocalDate.mondayOfSameWeek(): LocalDate = minusDays(dayOfWeek.value - 1L)
fun LocalDate.atNormalStartTime(): OffsetDateTime = atTime(LocalTime.of(18, 30)).atOffset(ZoneOffset.UTC)
