package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class DayType(private val daysOfWeek: Set<DayOfWeek>) {
    MONDAYS(setOf(DayOfWeek.MONDAY)),
    TUESDAYS(setOf(DayOfWeek.TUESDAY)),
    WEDNESDAYS(setOf(DayOfWeek.WEDNESDAY)),
    THURSDAYS(setOf(DayOfWeek.THURSDAY)),
    FRIDAYS(setOf(DayOfWeek.FRIDAY)),
    SATURDAYS(setOf(DayOfWeek.SATURDAY)),
    SUNDAYS(setOf(DayOfWeek.SUNDAY)),
    WEEKDAYS(
        setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    ),
    WEEKENDS(
        setOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    ),
    EVERY_DAY(
        setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    );

    fun includes(localDate: LocalDate) = localDate.dayOfWeek in daysOfWeek
}
