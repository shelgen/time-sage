package com.github.shelgen.timesage

import java.time.*

fun nextMonday() = ZonedDateTime.now(ZoneOffset.UTC).plusDays(7).toLocalDate().mondayOfSameWeek()
fun weekDatesStartingWith(startDate: LocalDate) = (0L..6L).map(startDate::plusDays)
fun LocalDate.mondayOfSameWeek(): LocalDate = minusDays(dayOfWeek.value - 1L)
fun LocalDate.atNormalStartTime(): OffsetDateTime = atTime(LocalTime.of(18, 30)).atOffset(ZoneOffset.UTC)
