package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.time.TimeSlot

open class AvailabilityResponse(
    open val sessionLimit: Int,
    open val slotAvailabilities: Map<TimeSlot, Availability>,
) {
    operator fun get(timeSlot: TimeSlot): Availability? = slotAvailabilities[timeSlot]

    override fun hashCode(): Int {
        var result = sessionLimit
        result = 31 * result + slotAvailabilities.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AvailabilityResponse) return false
        return sessionLimit == other.sessionLimit && slotAvailabilities == other.slotAvailabilities
    }
}

class MutableAvailabilityResponse(
    override var sessionLimit: Int,
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
        fun createNew(planSessionLimit: Int) = MutableAvailabilityResponse(
            sessionLimit = planSessionLimit,
            slotAvailabilities = mutableMapOf(),
        )
    }
}
