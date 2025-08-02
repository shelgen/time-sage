package com.github.shelgen.timesage.domain

import java.time.DayOfWeek

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
