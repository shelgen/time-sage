package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordThreadChannelId
import com.github.shelgen.timesage.time.DateRange
import java.time.Instant

sealed interface AvailabilityInterface {
    val postedAt: Instant
    fun toLink(tenant: Tenant): String
}

data class AvailabilityMessage(
    override val postedAt: Instant,
    val message: DiscordMessageId,
) : AvailabilityInterface {
    override fun toLink(tenant: Tenant) =
        "https://discord.com/channels/${tenant.server}/${tenant.channel}/${message.id}"
}

data class AvailabilityThread(
    override val postedAt: Instant,
    val threadStartMessage: DiscordMessageId,
    val threadChannel: DiscordThreadChannelId,
    val periodLevelMessage: DiscordMessageId?,
    val weekMessages: Map<DateRange, DiscordMessageId>,
) : AvailabilityInterface {
    override fun toLink(tenant: Tenant) =
        "https://discord.com/channels/${tenant.server}/${threadChannel.id}"
}
