package com.github.shelgen.timesage.domain

import java.time.LocalDate

data class Week(
    val startDate: LocalDate,
    val messageDiscordId: Long?,
    val responses: Map<Long, Response>,
) {
    data class Response(
        val sessionLimit: Int?,
        val availability: Map<LocalDate, AvailabilityStatus>
    )
}
