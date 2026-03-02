package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

data class DatePeriod(
    val fromDate: LocalDate,
    val toDate: LocalDate
) {
    fun dates(): List<LocalDate> = fromDate.datesUntil(toDate.plusDays(1)).toList()
    override fun toString() = "$fromDate through $toDate"

    companion object {
        fun weekFrom(startDate: LocalDate) =
            DatePeriod(startDate, startDate.plusDays(6))

        fun monthFrom(yearMonth: YearMonth) =
            DatePeriod(yearMonth.atDay(1), yearMonth.atEndOfMonth())
    }
}

open class Scheduling(
    open val type: SchedulingType,
    open val startDayOfWeek: DayOfWeek,
    open val timeSlotRules: TimeSlotRules,
    open val daysBeforePeriod: Int,
    open val planningStartHour: Int,
    open val reminderIntervalDays: Int,
) {
    companion object {
        val DEFAULT = Scheduling(
            type = SchedulingType.WEEKLY,
            startDayOfWeek = DayOfWeek.MONDAY,
            timeSlotRules = TimeSlotRules.DEFAULT,
            daysBeforePeriod = 5,
            planningStartHour = 17,
            reminderIntervalDays = 1,
        )
    }

    fun getTimeSlots(datePeriod: DatePeriod, timeZone: TimeZone) =
        timeSlotRules.getTimeSlots(datePeriod, timeZone)

    fun activePeriod(timeZone: TimeZone): DatePeriod {
        val today = LocalDate.now(timeZone.toZoneId())
        val lookAhead = today.plusDays(daysBeforePeriod.toLong())
        return when (type) {
            SchedulingType.WEEKLY -> {
                val daysBack =
                    (DayOfWeek.entries.size + lookAhead.dayOfWeek.value - startDayOfWeek.value) % DayOfWeek.entries.size
                DatePeriod.weekFrom(lookAhead.minusDays(daysBack.toLong()))
            }

            SchedulingType.MONTHLY -> DatePeriod.monthFrom(YearMonth.from(lookAhead))
        }
    }
}

class MutableScheduling(
    override var type: SchedulingType,
    override var startDayOfWeek: DayOfWeek,
    override var timeSlotRules: TimeSlotRules,
    override var daysBeforePeriod: Int,
    override var planningStartHour: Int,
    override var reminderIntervalDays: Int,
) : Scheduling(
    type = type,
    startDayOfWeek = startDayOfWeek,
    timeSlotRules = timeSlotRules,
    daysBeforePeriod = daysBeforePeriod,
    planningStartHour = planningStartHour,
    reminderIntervalDays = reminderIntervalDays,
) {
    constructor(scheduling: Scheduling) : this(
        type = scheduling.type,
        startDayOfWeek = scheduling.startDayOfWeek,
        timeSlotRules = scheduling.timeSlotRules,
        daysBeforePeriod = scheduling.daysBeforePeriod,
        planningStartHour = scheduling.planningStartHour,
        reminderIntervalDays = scheduling.reminderIntervalDays,
    )
}
