package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalDate

open class AvailabilitiesPeriod(
    open val messageId: Long?,
    open val threadId: Long?,
    open val responses: UserResponses,
    open val concluded: Boolean,
    open val conclusionMessageId: Long?,
    open val lastReminderDate: LocalDate?,
) {
    companion object {
        val DEFAULT = AvailabilitiesPeriod(
            messageId = null,
            threadId = null,
            responses = UserResponses.NONE,
            concluded = false,
            conclusionMessageId = null,
            lastReminderDate = null,
        )
    }
}

class MutableAvailabilitiesPeriod(
    override var messageId: Long?,
    override var threadId: Long?,
    override val responses: MutableUserResponses,
    override var concluded: Boolean,
    override var conclusionMessageId: Long?,
    override var lastReminderDate: LocalDate?,
) : AvailabilitiesPeriod(messageId, threadId, responses, concluded, conclusionMessageId, lastReminderDate) {
    constructor(period: AvailabilitiesPeriod) : this(
        messageId = period.messageId,
        threadId = period.threadId,
        responses = MutableUserResponses(userResponses = period.responses),
        concluded = period.concluded,
        conclusionMessageId = period.conclusionMessageId,
        lastReminderDate = period.lastReminderDate,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrSetForUserId(userId).availabilities.setForTimeSlot(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrSetForUserId(userId).sessionLimit = sessionLimit
    }
}
