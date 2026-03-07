package com.github.shelgen.timesage.domain

import java.time.Instant

open class AvailabilityResponseDate(
    open val map: Map<Instant, AvailabilityStatus>
) {
    operator fun get(timeSlot: Instant): AvailabilityStatus? = map[timeSlot]
}

class MutableAvailabilityResponseDate(
    override val map: MutableMap<Instant, AvailabilityStatus>
) : AvailabilityResponseDate(map) {
    constructor(userResponses: AvailabilityResponseDate) : this(
        map = userResponses.map.toMutableMap()
    )

    operator fun set(timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        map[timeSlot] = availabilityStatus
    }
}
