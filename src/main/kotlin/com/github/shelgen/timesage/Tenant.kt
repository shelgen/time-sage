package com.github.shelgen.timesage

import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId

data class Tenant(
    val server: DiscordServerId,
    val textChannel: DiscordTextChannelId
)
