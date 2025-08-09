package com.github.shelgen.timesage.domain

import java.time.Instant

open class DateAvailabilities(
    open val map: Map<Instant, AvailabilityStatus>
) {
    fun forTimeSlot(timeSlot: Instant): AvailabilityStatus? = map[timeSlot]
}

class MutableDateAvailabilities(
    override val map: MutableMap<Instant, AvailabilityStatus>
) : DateAvailabilities(map) {
    constructor(userResponses: DateAvailabilities) : this(
        map = userResponses.map.toMutableMap()
    )

    fun setForTimeSlot(timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        map[timeSlot] = availabilityStatus
    }
}
