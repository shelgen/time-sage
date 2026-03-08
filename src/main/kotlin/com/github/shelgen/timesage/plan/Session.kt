package com.github.shelgen.timesage.plan

import com.github.shelgen.timesage.time.TimeSlot
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.discord.DiscordUserId

data class Session(
    val timeSlot: TimeSlot,
    val activityId: ActivityId,
    val participants: Set<Participant>,
    val missingOptionalCount: Int
) {
    private val participantUsers = participants.map { it.user }.toSet()

    fun hasParticipant(user: DiscordUserId) = user in participantUsers
    fun countIfNeedBes() = participants.count { it.ifNeedBe }
    fun countParticipants() = participants.size
}
