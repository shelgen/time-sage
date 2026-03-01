package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalDate

open class AvailabilitiesMonth(
    open val headerMessageId: Long?,
    open val threadId: Long?,
    open val responses: UserResponses,
    open val concluded: Boolean,
    open val conclusionMessageId: Long?,
    open val lastReminderDate: LocalDate?,
) {
    companion object {
        val DEFAULT = AvailabilitiesMonth(
            headerMessageId = null,
            threadId = null,
            responses = UserResponses.NONE,
            concluded = false,
            conclusionMessageId = null,
            lastReminderDate = null,
        )
    }
}

class MutableAvailabilitiesMonth(
    override var headerMessageId: Long?,
    override var threadId: Long?,
    override val responses: MutableUserResponses,
    override var concluded: Boolean,
    override var conclusionMessageId: Long?,
    override var lastReminderDate: LocalDate?,
) : AvailabilitiesMonth(headerMessageId, threadId, responses, concluded, conclusionMessageId, lastReminderDate) {
    constructor(month: AvailabilitiesMonth) : this(
        headerMessageId = month.headerMessageId,
        threadId = month.threadId,
        responses = MutableUserResponses(userResponses = month.responses),
        concluded = month.concluded,
        conclusionMessageId = month.conclusionMessageId,
        lastReminderDate = month.lastReminderDate,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrSetForUserId(userId).availabilities.setForTimeSlot(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrSetForUserId(userId).sessionLimit = sessionLimit
    }
}
