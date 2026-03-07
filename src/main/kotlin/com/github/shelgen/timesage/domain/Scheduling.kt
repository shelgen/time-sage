package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

open class Scheduling(
    open val type: SchedulingType,
    open val timeSlotRules: TimeSlotRules,
    open val numDaysInAdvanceToStartPlanning: Int,
    open val timeOfDayToStartPlanning: Int,
    open val reminderIntervalDays: Int,
) {
    companion object {
        val DEFAULT = Scheduling(
            type = SchedulingType.WEEKLY,
            timeSlotRules = TimeSlotRules.DEFAULT,
            numDaysInAdvanceToStartPlanning = 5,
            timeOfDayToStartPlanning = 17,
            reminderIntervalDays = 1,
        )
    }
}

class MutableScheduling(
    override var type: SchedulingType,
    override var timeSlotRules: TimeSlotRules,
    override var numDaysInAdvanceToStartPlanning: Int,
    override var timeOfDayToStartPlanning: Int,
    override var reminderIntervalDays: Int,
) : Scheduling(
    type = type,
    timeSlotRules = timeSlotRules,
    numDaysInAdvanceToStartPlanning = numDaysInAdvanceToStartPlanning,
    timeOfDayToStartPlanning = timeOfDayToStartPlanning,
    reminderIntervalDays = reminderIntervalDays,
) {
    constructor(scheduling: Scheduling) : this(
        type = scheduling.type,
        timeSlotRules = scheduling.timeSlotRules,
        numDaysInAdvanceToStartPlanning = scheduling.numDaysInAdvanceToStartPlanning,
        timeOfDayToStartPlanning = scheduling.timeOfDayToStartPlanning,
        reminderIntervalDays = scheduling.reminderIntervalDays,
    )
}
