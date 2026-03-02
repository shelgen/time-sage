package com.github.shelgen.timesage.domain

sealed interface AvailabilityMessageOrThread {
    data class AvailabilityThread(
        val headerMessageId: Long,
        val threadId: Long,
        val sessionLimitAndUnavailableMessageId: Long?,
        val availabilityMessageIds: Map<DatePeriod, Long>,
    ) : AvailabilityMessageOrThread

    data class AvailabilityMessage(
        val messageId: Long,
    ) : AvailabilityMessageOrThread
}
