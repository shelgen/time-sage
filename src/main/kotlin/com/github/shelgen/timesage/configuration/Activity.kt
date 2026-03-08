package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.discord.DiscordVoiceChannelId

data class ActivityId(val value: Int)

open class Activity(
    val id: ActivityId,
    open val name: String,
    open val members: List<Member>,
    open val maxNumMissingOptionalMembers: Int,
    open val voiceChannel: DiscordVoiceChannelId?,
) {
    fun isRequiredMember(user: DiscordUserId) = members.any { it.user == user && !it.optional }
    fun isMember(user: DiscordUserId) = members.any { it.user == user }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + members.hashCode()
        result = 31 * result + maxNumMissingOptionalMembers
        result = 31 * result + (voiceChannel?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Activity) return false
        return id == other.id &&
                name == other.name &&
                members == other.members &&
                maxNumMissingOptionalMembers == other.maxNumMissingOptionalMembers &&
                voiceChannel == other.voiceChannel
    }
}

class MutableActivity(
    id: ActivityId,
    override var name: String,
    override val members: MutableList<MutableMember>,
    override var maxNumMissingOptionalMembers: Int,
    override var voiceChannel: DiscordVoiceChannelId?,
) : Activity(
    id = id,
    name = name,
    members = members,
    maxNumMissingOptionalMembers = maxNumMissingOptionalMembers,
    voiceChannel = voiceChannel,
) {
    constructor(immutable: Activity) : this(
        id = immutable.id,
        name = immutable.name,
        members = immutable.members.map(::MutableMember).toMutableList(),
        maxNumMissingOptionalMembers = immutable.maxNumMissingOptionalMembers,
        voiceChannel = immutable.voiceChannel,
    )

    fun setRequiredMembers(users: List<DiscordUserId>) {
        members.removeIf { !it.optional }
        users.distinct().forEach { userId ->
            val member = members.firstOrNull { it.user == userId }
            if (member == null) {
                members.add(MutableMember(user = userId, optional = false))
            } else {
                member.optional = false
            }
        }
    }

    fun setOptionalMembers(users: List<DiscordUserId>) {
        members.removeIf { it.optional }
        users.distinct().forEach { userId ->
            val member = members.firstOrNull { it.user == userId }
            if (member == null) {
                members.add(MutableMember(user = userId, optional = true))
            } else {
                member.optional = true
            }
        }
    }

    companion object {
        fun createNew(id: ActivityId) = MutableActivity(
            id = id,
            name = "New Activity",
            members = mutableListOf(),
            maxNumMissingOptionalMembers = 0,
            voiceChannel = null,
        )
    }
}
