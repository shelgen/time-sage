package com.github.shelgen.timesage.domain

open class Participant(
    open val userId: Long,
    open val optional: Boolean
)

class MutableParticipant(
    userId: Long,
    override var optional: Boolean
) : Participant(
    userId = userId,
    optional = optional
) {
    constructor(participant: Participant) : this(
        userId = participant.userId,
        optional = participant.optional
    )
}
