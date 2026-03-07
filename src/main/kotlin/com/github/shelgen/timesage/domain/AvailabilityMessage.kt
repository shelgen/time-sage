package com.github.shelgen.timesage.domain

sealed interface AvailabilityMessage {
    data class Thread(
        val threadStartScreenMessageId: Long,
        val threadChannelId: Long,
        val periodLevelScreenMessageId: Long?,
        val availabilityWeekScreenMessageIds: Map<DateRange, Long>,
    ) : AvailabilityMessage

    data class SingleMessage(
        val messageId: Long,
    ) : AvailabilityMessage
}
