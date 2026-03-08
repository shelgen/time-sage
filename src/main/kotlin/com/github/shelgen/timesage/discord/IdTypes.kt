package com.github.shelgen.timesage.discord

@JvmInline
value class DiscordVoiceChannelId(val id: Long)

@JvmInline
value class DiscordServerId(val id: Long)

@JvmInline
value class DiscordTextChannelId(val id: Long) {
    fun toMention() = "<#$id>"
}

@JvmInline
value class DiscordUserId(val id: Long) {
    fun toMention() = "<@${id}>"
}

@JvmInline
value class DiscordMessageId(val id: Long)

@JvmInline
value class DiscordThreadChannelId(val id: Long)
