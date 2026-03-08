package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.time.TimeSlot

open class AvailabilityResponse(
    open val sessionLimit: Int?,
    open val slotAvailabilities: Map<TimeSlot, Availability>,
) {
    operator fun get(timeSlot: TimeSlot): Availability? = slotAvailabilities[timeSlot]
}

class MutableAvailabilityResponse(
    override var sessionLimit: Int?,
    override val slotAvailabilities: MutableMap<TimeSlot, Availability>,
) : AvailabilityResponse(
    sessionLimit = sessionLimit,
    slotAvailabilities = slotAvailabilities,
) {
    constructor(availabilityResponse: AvailabilityResponse) : this(
        sessionLimit = availabilityResponse.sessionLimit,
        slotAvailabilities = availabilityResponse.slotAvailabilities.toMutableMap(),
    )

    operator fun set(timeSlot: TimeSlot, availability: Availability) {
        slotAvailabilities[timeSlot] = availability
    }

    companion object {
        fun createNew() = MutableAvailabilityResponse(
            sessionLimit = null,
            slotAvailabilities = mutableMapOf(),
        )
    }
}
