package com.github.shelgen.timesage.domain

open class AvailabilityResponses(
    open val map: Map<Long, AvailabilityResponse>
) {
    open operator fun get(userId: Long): AvailabilityResponse? = map[userId]

    companion object {
        val NONE = AvailabilityResponses(map = emptyMap())
    }
}

class MutableAvailabilityResponses(
    override val map: MutableMap<Long, MutableAvailabilityResponse>
) : AvailabilityResponses(map) {
    constructor(availabilityResponses: AvailabilityResponses) : this(
        map = availabilityResponses.map
            .map { (userId, response) -> userId to MutableAvailabilityResponse(response) }
            .toMap()
            .toMutableMap()
    )

    override operator fun get(userId: Long): MutableAvailabilityResponse? = map[userId]
    fun getOrInitialize(userId: Long): MutableAvailabilityResponse = map.getOrPut(userId) { MutableAvailabilityResponse.createNew() }
}
