package com.github.shelgen.timesage.domain

sealed interface AvailabilityMessageOrThread {
    data class AvailabilityThread(
        val threadStartScreenMessageId: Long,
        val threadChannelId: Long,
        val periodLevelScreenMessageId: Long?,
        val availabilityWeekScreenMessageIds: Map<DateRange, Long>,
    ) : AvailabilityMessageOrThread

    data class AvailabilityMessage(
        val screenMessageId: Long,
    ) : AvailabilityMessageOrThread
}
