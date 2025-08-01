package com.github.shelgen.timesage.domain

import java.time.DayOfWeek

open class Scheduling(
    open val type: SchedulingType,
    open val startDayOfWeek: DayOfWeek
) {
    companion object {
        val DEFAULT = Scheduling(type = SchedulingType.WEEKLY, startDayOfWeek = DayOfWeek.MONDAY)
    }
}

class MutableScheduling(
    override var type: SchedulingType,
    override var startDayOfWeek: DayOfWeek
) : Scheduling(
    type = type,
    startDayOfWeek = startDayOfWeek
) {
    constructor(scheduling: Scheduling) : this(
        type = scheduling.type,
        startDayOfWeek = scheduling.startDayOfWeek
    )
}
