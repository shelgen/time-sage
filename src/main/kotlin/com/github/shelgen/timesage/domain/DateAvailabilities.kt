package com.github.shelgen.timesage.domain

import java.time.LocalDate

open class DateAvailabilities(
    open val map: Map<LocalDate, AvailabilityStatus>
) {
    fun forDate(date: LocalDate): AvailabilityStatus? = map[date]
}

class MutableDateAvailabilities(
    override val map: MutableMap<LocalDate, AvailabilityStatus>
) : DateAvailabilities(map) {
    constructor(userResponses: DateAvailabilities) : this(
        map = userResponses.map.toMutableMap()
    )

    fun setForDate(date: LocalDate, availabilityStatus: AvailabilityStatus) {
        map[date] = availabilityStatus
    }
}
