package com.github.shelgen.timesage.plan

import com.github.shelgen.timesage.discord.DiscordUserId

data class Participant(
    val user: DiscordUserId,
    val ifNeedBe: Boolean
) {
    override fun equals(other: Any?) = this.user == (other as? Participant)?.user
    override fun hashCode() = user.hashCode()
}
