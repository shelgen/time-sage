package com.github.shelgen.timesage.domain

import java.time.LocalDate

class AvailabilitiesWeek(
    val messageId: Long?,
    val responses: Map<Long, Response>,
) {
    data class Response(
        val sessionLimit: Int?,
        val availabilities: Map<LocalDate, AvailabilityStatus>
    )

    companion object {
        val DEFAULT = AvailabilitiesWeek(
            messageId = null,
            responses = emptyMap()
        )
    }
}
