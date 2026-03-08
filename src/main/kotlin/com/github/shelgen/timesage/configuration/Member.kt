package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.discord.DiscordUserId

open class Member(
    open val user: DiscordUserId,
    open val optional: Boolean
) {
    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + optional.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Member) return false
        return user == other.user && optional == other.optional
    }
}

class MutableMember(
    user: DiscordUserId,
    override var optional: Boolean
) : Member(user, optional) {
    constructor(immutable: Member) : this(
        user = immutable.user,
        optional = immutable.optional
    )
}
