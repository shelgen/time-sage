package com.github.shelgen.timesage.planning

import java.time.Instant

data class PlannedSession(
    val timeSlot: Instant,
    val activityId: Int,
    val participants: Set<Participant>,
    val missingOptionalCount: Int
) {
    data class Participant(val userId: Long, val ifNeedBe: Boolean)

    private val participantUserIds: Set<Long> = participants.map(Participant::userId).toSet()

    fun hasParticipant(userId: Long) = userId in participantUserIds
}
