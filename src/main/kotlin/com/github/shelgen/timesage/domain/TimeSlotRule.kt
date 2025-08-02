package com.github.shelgen.timesage.domain

import java.time.LocalTime

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
}
