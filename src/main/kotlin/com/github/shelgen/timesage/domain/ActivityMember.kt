package com.github.shelgen.timesage.domain

open class ActivityMember(
    open val userId: Long,
    open val optional: Boolean
)

class MutableActivityMember(
    userId: Long,
    override var optional: Boolean
) : ActivityMember(
    userId = userId,
    optional = optional
) {
    constructor(activityMember: ActivityMember) : this(
        userId = activityMember.userId,
        optional = activityMember.optional
    )
}
