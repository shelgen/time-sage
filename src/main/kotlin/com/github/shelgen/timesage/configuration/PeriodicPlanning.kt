package com.github.shelgen.timesage.configuration

open class PeriodicPlanning(
    open val enabled: Boolean,
    open val periodType: PeriodType,
    open val daysInAdvance: Int,
    open val hourOfDay: Int,
) {
    fun nextPeriod(localization: Localization) = periodType.nextPeriod(localization)

    companion object {
        val DEFAULT = PeriodicPlanning(
            enabled = false,
            periodType = PeriodType.WEEKLY,
            daysInAdvance = 5,
            hourOfDay = 17,
        )
    }
}

class MutablePeriodicPlanning(
    override var enabled: Boolean,
    override var periodType: PeriodType,
    override var daysInAdvance: Int,
    override var hourOfDay: Int,
) : PeriodicPlanning(enabled, periodType, daysInAdvance, hourOfDay) {
    constructor(periodicPlanning: PeriodicPlanning) : this(
        enabled = periodicPlanning.enabled,
        periodType = periodicPlanning.periodType,
        daysInAdvance = periodicPlanning.daysInAdvance,
        hourOfDay = periodicPlanning.hourOfDay,
    )
}
