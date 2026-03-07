package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalDate

open class AvailabilitiesPeriod(
    open val availabilityMessageOrThread: AvailabilityMessageOrThread?,
    open val responses: UserResponses,
    open val concluded: Boolean,
    open val conclusionMessageId: Long?,
    open val lastReminderDate: LocalDate?,
) {
    companion object {
        val DEFAULT = AvailabilitiesPeriod(
            availabilityMessageOrThread = null,
            responses = UserResponses.NONE,
            concluded = false,
            conclusionMessageId = null,
            lastReminderDate = null,
        )
    }
}

class MutableAvailabilitiesPeriod(
    override var availabilityMessageOrThread: AvailabilityMessageOrThread?,
    override val responses: MutableUserResponses,
    override var concluded: Boolean,
    override var conclusionMessageId: Long?,
    override var lastReminderDate: LocalDate?,
) : AvailabilitiesPeriod(availabilityMessageOrThread, responses, concluded, conclusionMessageId, lastReminderDate) {
    constructor(period: AvailabilitiesPeriod) : this(
        availabilityMessageOrThread = period.availabilityMessageOrThread,
        responses = MutableUserResponses(userResponses = period.responses),
        concluded = period.concluded,
        conclusionMessageId = period.conclusionMessageId,
        lastReminderDate = period.lastReminderDate,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        responses.getOrInitialize(userId).availabilities.set(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        responses.getOrInitialize(userId).sessionLimit = sessionLimit
    }
}
