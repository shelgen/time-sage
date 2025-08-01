package com.github.shelgen.timesage.domain

open class UserResponse(
    open val sessionLimit: Int?,
    open val availabilities: DateAvailabilities
)

class MutableUserResponse(
    override var sessionLimit: Int?,
    override val availabilities: MutableDateAvailabilities
) : UserResponse(
    sessionLimit = sessionLimit,
    availabilities = availabilities
) {
    constructor(userResponse: UserResponse) : this(
        sessionLimit = userResponse.sessionLimit,
        availabilities = MutableDateAvailabilities(userResponses = userResponse.availabilities)
    )

    companion object {
        fun createNew() = MutableUserResponse(
            sessionLimit = null,
            availabilities = MutableDateAvailabilities(mutableMapOf())
        )
    }
}
