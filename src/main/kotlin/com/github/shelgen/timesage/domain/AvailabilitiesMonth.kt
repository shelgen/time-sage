package com.github.shelgen.timesage.domain

import java.time.Instant

open class AvailabilitiesMonth(
    open val headerMessageId: Long?,
    open val threadId: Long?,
    open val responses: UserResponses,
    open val concluded: Boolean,
    open val conclusionMessageId: Long?,
) {
    companion object {
        val DEFAULT = AvailabilitiesMonth(
            headerMessageId = null,
            threadId = null,
            responses = UserResponses.NONE,
            concluded = false,
            conclusionMessageId = null,
        )
    }
}

class MutableAvailabilitiesMonth(
    override var headerMessageId: Long?,
    override var threadId: Long?,
    override val responses: MutableUserResponses,
    override var concluded: Boolean,
    override var conclusionMessageId: Long?,
) : AvailabilitiesMonth(headerMessageId, threadId, responses, concluded, conclusionMessageId) {
    constructor(month: AvailabilitiesMonth) : this(
        headerMessageId = month.headerMessageId,
        threadId = month.threadId,
        responses = MutableUserResponses(userResponses = month.responses),
        concluded = month.concluded,
        conclusionMessageId = month.conclusionMessageId,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrSetForUserId(userId).availabilities.setForTimeSlot(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrSetForUserId(userId).sessionLimit = sessionLimit
    }
}
