package com.github.shelgen.timesage.domain

open class Activity(
    val id: Int,
    open val name: String,
    open val members: List<ActivityMember>,
    open val maxMissingOptionalMembers: Int
) {
    fun userIsRequiredMember(userId: Long) = members.any { it.userId == userId && !it.optional }
    fun userIsMember(userId: Long) = members.any { it.userId == userId }
}

class MutableActivity(
    id: Int,
    override var name: String,
    override val members: MutableList<MutableActivityMember>,
    override var maxMissingOptionalMembers: Int
) : Activity(
    id = id,
    name = name,
    members = members,
    maxMissingOptionalMembers = maxMissingOptionalMembers
) {
    constructor(activity: Activity) : this(
        id = activity.id,
        name = activity.name,
        members = activity.members.map(::MutableActivityMember).toMutableList(),
        maxMissingOptionalMembers = activity.maxMissingOptionalMembers
    )

    fun setRequiredParticipants(userIds: List<Long>) {
        members.removeIf { !it.optional }
        userIds.distinct().forEach { userId ->
            val participant = members.firstOrNull { it.userId == userId }
            if (participant == null) {
                members.add(MutableActivityMember(userId = userId, optional = false))
            } else {
                participant.optional = false
            }
        }
    }

    fun setOptionalParticipants(userIds: List<Long>) {
        members.removeIf { it.optional }
        userIds.distinct().forEach { userId ->
            val participant = members.firstOrNull { it.userId == userId }
            if (participant == null) {
                members.add(MutableActivityMember(userId = userId, optional = true))
            } else {
                participant.optional = true
            }
        }
    }

    companion object {
        fun createNew(id: Int) = MutableActivity(
            id = id,
            name = "New Activity",
            members = mutableListOf(),
            maxMissingOptionalMembers = 0
        )
    }
}
