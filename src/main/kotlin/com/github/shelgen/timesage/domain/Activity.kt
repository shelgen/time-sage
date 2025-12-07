package com.github.shelgen.timesage.domain

open class Activity(
    val id: Int,
    open val name: String,
    open val participants: List<Participant>,
    open val maxMissingOptionalParticipants: Int
) {
    fun isRequiredParticipant(userId: Long) = participants.any { it.userId == userId && !it.optional }
    fun hasParticipant(userId: Long) = participants.any { it.userId == userId }
}

class MutableActivity(
    id: Int,
    override var name: String,
    override val participants: MutableList<MutableParticipant>,
    override var maxMissingOptionalParticipants: Int
) : Activity(
    id = id,
    name = name,
    participants = participants,
    maxMissingOptionalParticipants = maxMissingOptionalParticipants
) {
    constructor(activity: Activity) : this(
        id = activity.id,
        name = activity.name,
        participants = activity.participants.map(::MutableParticipant).toMutableList(),
        maxMissingOptionalParticipants = activity.maxMissingOptionalParticipants
    )

    fun setRequiredParticipants(userIds: List<Long>) {
        participants.removeIf { !it.optional }
        userIds.distinct().forEach { userId ->
            val participant = participants.firstOrNull { it.userId == userId }
            if (participant == null) {
                participants.add(MutableParticipant(userId = userId, optional = false))
            } else {
                participant.optional = false
            }
        }
    }

    fun setOptionalParticipants(userIds: List<Long>) {
        participants.removeIf { it.optional }
        userIds.distinct().forEach { userId ->
            val participant = participants.firstOrNull { it.userId == userId }
            if (participant == null) {
                participants.add(MutableParticipant(userId = userId, optional = true))
            } else {
                participant.optional = true
            }
        }
    }

    companion object {
        fun createNew(id: Int) = MutableActivity(
            id = id,
            name = "New Activity",
            participants = mutableListOf(),
            maxMissingOptionalParticipants = 0
        )
    }
}
