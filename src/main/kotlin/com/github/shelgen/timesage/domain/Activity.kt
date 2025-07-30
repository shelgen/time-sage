package com.github.shelgen.timesage.domain

data class Activity(
    val id: Int,
    val name: String,
    val participants: List<Participant>,
    val maxMissingOptionalParticipants: Int
) {
    fun isRequiredParticipant(userId: Long) = participants.any { it.userId == userId && !it.optional }
    fun hasParticipant(userId: Long) = participants.any { it.userId == userId }
}
