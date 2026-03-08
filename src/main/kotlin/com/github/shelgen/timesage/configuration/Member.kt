package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.discord.DiscordUserId

open class Member(
    open val user: DiscordUserId,
    open val optional: Boolean
)

class MutableMember(
    user: DiscordUserId,
    override var optional: Boolean
) : Member(user, optional) {
    constructor(immutable: Member) : this(
        user = immutable.user,
        optional = immutable.optional
    )
}
