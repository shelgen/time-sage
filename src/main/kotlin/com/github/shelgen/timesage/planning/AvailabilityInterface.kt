package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordThreadChannelId
import java.time.Instant

sealed interface AvailabilityInterface {
    val postedAt: Instant
    fun toLink(tenant: Tenant): String
    fun messageToPin(): DiscordMessageId
}

data class AvailabilityMessage(
    override val postedAt: Instant,
    val message: DiscordMessageId,
) : AvailabilityInterface {
    override fun toLink(tenant: Tenant) =
        "https://discord.com/channels/${tenant.server.id}/${tenant.textChannel.id}/${message.id}"

    override fun messageToPin(): DiscordMessageId = message
}

data class AvailabilityThread(
    override val postedAt: Instant,
    val threadStartMessage: DiscordMessageId,
    val threadChannel: DiscordThreadChannelId,
    val periodLevelMessage: DiscordMessageId,
    val timeSlotChunks: List<TimeSlotChunk>,
) : AvailabilityInterface {
    data class TimeSlotChunk(val size: Int, val message: DiscordMessageId)

    override fun toLink(tenant: Tenant) =
        "https://discord.com/channels/${tenant.server.id}/${threadChannel.id}/${periodLevelMessage.id}"

    override fun messageToPin(): DiscordMessageId = threadStartMessage
}
