package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.discord.DiscordUserId

open class AvailabilityResponses(
    open val map: Map<DiscordUserId, AvailabilityResponse>
) {
    open operator fun get(userId: DiscordUserId): AvailabilityResponse? = map[userId]

    companion object {
        val NONE = AvailabilityResponses(map = emptyMap())
    }
}

class MutableAvailabilityResponses(
    override val map: MutableMap<DiscordUserId, MutableAvailabilityResponse>
) : AvailabilityResponses(map) {
    constructor(availabilityResponses: AvailabilityResponses) : this(
        map = availabilityResponses.map
            .map { (userId, response) -> userId to MutableAvailabilityResponse(response) }
            .toMap()
            .toMutableMap()
    )

    override operator fun get(userId: DiscordUserId): MutableAvailabilityResponse? = map[userId]
    fun getOrInitialize(userId: DiscordUserId): MutableAvailabilityResponse = map.getOrPut(userId) { MutableAvailabilityResponse.createNew() }
}
