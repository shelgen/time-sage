package com.github.shelgen.timesage.domain

import java.time.Instant
import java.time.LocalDate

open class PlanningTargetPeriod(
    open val availabilityMessageOrThread: AvailabilityMessageOrThread?,
    open val availabilityResponses: AvailabilityResponses,
    open val conclusionMessageId: Long?,
    open val lastReminderDate: LocalDate?,
) {
    val concluded: Boolean get() = conclusionMessageId != null

    companion object {
        val DEFAULT = PlanningTargetPeriod(
            availabilityMessageOrThread = null,
            availabilityResponses = AvailabilityResponses.NONE,
            conclusionMessageId = null,
            lastReminderDate = null,
        )
    }
}

class MutablePlanningTargetPeriod(
    override var availabilityMessageOrThread: AvailabilityMessageOrThread?,
    override val availabilityResponses: MutableAvailabilityResponses,
    override var conclusionMessageId: Long?,
    override var lastReminderDate: LocalDate?,
) : PlanningTargetPeriod(availabilityMessageOrThread, availabilityResponses, conclusionMessageId, lastReminderDate) {
    constructor(period: PlanningTargetPeriod) : this(
        availabilityMessageOrThread = period.availabilityMessageOrThread,
        availabilityResponses = MutableAvailabilityResponses(availabilityResponses = period.availabilityResponses),
        conclusionMessageId = period.conclusionMessageId,
        lastReminderDate = period.lastReminderDate,
    )

    fun setUserTimeSlotAvailability(userId: Long, timeSlot: Instant, availabilityStatus: AvailabilityStatus) {
        availabilityResponses.getOrInitialize(userId).dates.set(timeSlot, availabilityStatus)
    }

    fun setUserSessionLimit(userId: Long, sessionLimit: Int) {
        availabilityResponses.getOrInitialize(userId).sessionLimit = sessionLimit
    }
}
