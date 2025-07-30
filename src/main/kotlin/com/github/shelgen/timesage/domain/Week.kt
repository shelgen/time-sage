package com.github.shelgen.timesage.domain

import java.time.LocalDate

data class Week(
    val guildId: Long,
    val mondayDate: LocalDate,
    val weekAvailabilityMessageDiscordId: Long?,
    val playerResponses: Map<Long, PlayerResponse>,
) {
    data class PlayerResponse(
        val sessionLimit: Int?,
        val availability: Map<LocalDate, AvailabilityStatus>
    )
}
