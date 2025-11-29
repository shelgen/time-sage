package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.TimeZone

data class DatePeriod(
    val fromDate: LocalDate,
    val toDate: LocalDate
) {
    fun dates(): List<LocalDate> = fromDate.datesUntil(toDate.plusDays(1)).toList()

    companion object {
        fun weekFrom(startDate: LocalDate) =
            DatePeriod(startDate, startDate.plusDays(6))
    }
}

open class Scheduling(
    open val type: SchedulingType,
    open val startDayOfWeek: DayOfWeek,
    open val timeSlotRules: List<TimeSlotRule>
) {
    companion object {
        val DEFAULT = Scheduling(
            type = SchedulingType.WEEKLY,
            startDayOfWeek = DayOfWeek.MONDAY,
            timeSlotRules = listOf(TimeSlotRule.DEFAULT)
        )
    }

    fun getTimeSlots(datePeriod: DatePeriod, timeZone: TimeZone) =
        timeSlotRules.flatMap { it.getTimeSlots(datePeriod, timeZone) }.distinct().sorted()
}

class MutableScheduling(
    override var type: SchedulingType,
    override var startDayOfWeek: DayOfWeek,
    override var timeSlotRules: MutableList<TimeSlotRule>
) : Scheduling(
    type = type,
    startDayOfWeek = startDayOfWeek,
    timeSlotRules = timeSlotRules
) {
    constructor(scheduling: Scheduling) : this(
        type = scheduling.type,
        startDayOfWeek = scheduling.startDayOfWeek,
        timeSlotRules = scheduling.timeSlotRules.toMutableList()
    )
}
