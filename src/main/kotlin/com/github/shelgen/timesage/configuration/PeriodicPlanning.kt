package com.github.shelgen.timesage.configuration

open class PeriodicPlanning(
    open val enabled: Boolean,
    open val interval: Interval,
    open val daysInAdvance: Int,
    open val hourOfDay: Int,
) {
    fun nextPeriod(localization: Localization) = interval.nextDateRange(localization)

    companion object {
        val DEFAULT = PeriodicPlanning(
            enabled = false,
            interval = Interval.WEEKLY,
            daysInAdvance = 5,
            hourOfDay = 17,
        )
    }
}

class MutablePeriodicPlanning(
    override var enabled: Boolean,
    override var interval: Interval,
    override var daysInAdvance: Int,
    override var hourOfDay: Int,
) : PeriodicPlanning(enabled, interval, daysInAdvance, hourOfDay) {
    constructor(periodicPlanning: PeriodicPlanning) : this(
        enabled = periodicPlanning.enabled,
        interval = periodicPlanning.interval,
        daysInAdvance = periodicPlanning.daysInAdvance,
        hourOfDay = periodicPlanning.hourOfDay,
    )
}
