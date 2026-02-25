package com.github.shelgen.timesage.domain

import java.time.Instant

open class AvailabilitiesWeek(
    open val messageId: Long?,
    open val responses: UserResponses,
    open val concluded: Boolean,
    open val conclusionMessageId: Long?,
) {
    companion object {
        val DEFAULT = AvailabilitiesWeek(
            messageId = null,
            responses = UserResponses.NONE,
            concluded = false,
            conclusionMessageId = null,
        )
    }
}

class MutableAvailabilitiesWeek(
    override var messageId: Long?,
    override val responses: MutableUserResponses,
    override var concluded: Boolean,
    override var conclusionMessageId: Long?,
) : AvailabilitiesWeek(messageId, responses, concluded, conclusionMessageId) {
    constructor(week: AvailabilitiesWeek) : this(
        messageId = week.messageId,
        responses = MutableUserResponses(userResponses = week.responses),
        concluded = week.concluded,
        conclusionMessageId = week.conclusionMessageId,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrSetForUserId(userId).availabilities.setForTimeSlot(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrSetForUserId(userId).sessionLimit = sessionLimit
    }
}
