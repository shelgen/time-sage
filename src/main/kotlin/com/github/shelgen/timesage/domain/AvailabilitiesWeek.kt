package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalDate

open class AvailabilitiesWeek(
    open val messageId: Long?,
    open val responses: UserResponses,
    open val concluded: Boolean,
    open val conclusionMessageId: Long?,
    open val lastReminderDate: LocalDate?,
) {
    companion object {
        val DEFAULT = AvailabilitiesWeek(
            messageId = null,
            responses = UserResponses.NONE,
            concluded = false,
            conclusionMessageId = null,
            lastReminderDate = null,
        )
    }
}

class MutableAvailabilitiesWeek(
    override var messageId: Long?,
    override val responses: MutableUserResponses,
    override var concluded: Boolean,
    override var conclusionMessageId: Long?,
    override var lastReminderDate: LocalDate?,
) : AvailabilitiesWeek(messageId, responses, concluded, conclusionMessageId, lastReminderDate) {
    constructor(week: AvailabilitiesWeek) : this(
        messageId = week.messageId,
        responses = MutableUserResponses(userResponses = week.responses),
        concluded = week.concluded,
        conclusionMessageId = week.conclusionMessageId,
        lastReminderDate = week.lastReminderDate,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrSetForUserId(userId).availabilities.setForTimeSlot(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrSetForUserId(userId).sessionLimit = sessionLimit
    }
}
