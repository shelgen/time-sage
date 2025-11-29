package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class DayType(val humanReadableName: String, private val daysOfWeek: Set<DayOfWeek>) {
    MONDAYS("Mondays", setOf(DayOfWeek.MONDAY)),
    TUESDAYS("Tuesdays", setOf(DayOfWeek.TUESDAY)),
    WEDNESDAYS("Wednesdays", setOf(DayOfWeek.WEDNESDAY)),
    THURSDAYS("Thursdays", setOf(DayOfWeek.THURSDAY)),
    FRIDAYS("Fridays", setOf(DayOfWeek.FRIDAY)),
    SATURDAYS("Saturdays", setOf(DayOfWeek.SATURDAY)),
    SUNDAYS("Sundays", setOf(DayOfWeek.SUNDAY)),
    WEEKDAYS(
        "Weekdays",
        setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    ),
    WEEKENDS(
        "Weekends",
        setOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
    ),
    EVERY_DAY(
        "Every day",
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
