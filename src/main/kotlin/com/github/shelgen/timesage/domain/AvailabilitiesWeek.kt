package com.github.shelgen.timesage.domain

import java.time.Instant

open class AvailabilitiesWeek(
    open val messageId: Long?,
    open val responses: UserResponses
) {
    companion object {
        val DEFAULT = AvailabilitiesWeek(
            messageId = null,
            responses = UserResponses.NONE
        )
    }
}

class MutableAvailabilitiesWeek(
    override var messageId: Long?,
    override val responses: MutableUserResponses,
) : AvailabilitiesWeek(messageId, responses) {
    constructor(week: AvailabilitiesWeek) : this(
        messageId = week.messageId,
        responses = MutableUserResponses(userResponses = week.responses)
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrSetForUserId(userId).availabilities.setForTimeSlot(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrSetForUserId(userId).sessionLimit = sessionLimit
    }
}
