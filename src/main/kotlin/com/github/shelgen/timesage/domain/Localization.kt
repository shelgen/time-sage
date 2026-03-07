package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.util.*

open class Localization(
    open val timeZone: TimeZone,
    open val startDayOfWeek: DayOfWeek,
) {
    companion object {
        val DEFAULT = Localization(
            timeZone = TimeZone.getTimeZone("Europe/Berlin"),
            startDayOfWeek = DayOfWeek.MONDAY,
        )
    }
}

class MutableLocalization(
    override var timeZone: TimeZone,
    override var startDayOfWeek: DayOfWeek,
) : Localization(timeZone, startDayOfWeek) {
    constructor(localization: Localization) : this(
        timeZone = localization.timeZone,
        startDayOfWeek = localization.startDayOfWeek,
    )
}
