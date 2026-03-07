package com.github.shelgen.timesage.domain

open class UserResponses(
    open val map: Map<Long, UserResponse>
) {
    open operator fun get(userId: Long): UserResponse? = map[userId]

    companion object {
        val NONE = UserResponses(map = emptyMap())
    }
}

class MutableUserResponses(
    override val map: MutableMap<Long, MutableUserResponse>
) : UserResponses(map) {
    constructor(userResponses: UserResponses) : this(
        map = userResponses.map
            .map { (userId, response) -> userId to MutableUserResponse(response) }
            .toMap()
            .toMutableMap()
    )

    override operator fun get(userId: Long): MutableUserResponse? = map[userId]
    fun getOrInitialize(userId: Long): MutableUserResponse = map.getOrPut(userId) { MutableUserResponse.createNew() }
}
