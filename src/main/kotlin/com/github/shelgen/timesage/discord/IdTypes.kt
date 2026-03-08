package com.github.shelgen.timesage.discord

data class DiscordVoiceChannelId(val id: Long)

data class DiscordServerId(val id: Long)

data class DiscordTextChannelId(val id: Long) {
    fun toMention() = "<#$id>"
}

data class DiscordUserId(val id: Long) {
    fun toMention() = "<@${id}>"
}

data class DiscordMessageId(val id: Long)

data class DiscordThreadChannelId(val id: Long)
