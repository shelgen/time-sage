package com.github.shelgen.timesage.configuration

open class Reminders(
    open val enabled: Boolean,
    open val intervalDays: Int,
    open val hourOfDay: Int,
) {
    companion object {
        val DEFAULT = Reminders(enabled = true, intervalDays = 1, hourOfDay = 17)
    }
}

class MutableReminders(
    override var enabled: Boolean,
    override var intervalDays: Int,
    override var hourOfDay: Int,
) : Reminders(enabled, intervalDays, hourOfDay) {
    constructor(reminders: Reminders) : this(
        enabled = reminders.enabled,
        intervalDays = reminders.intervalDays,
        hourOfDay = reminders.hourOfDay,
    )
}
