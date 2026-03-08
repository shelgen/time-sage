package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.discord.DiscordMessageId
import java.time.Instant

data class SentReminder(
    val sentAt: Instant,
    val message: DiscordMessageId
)
