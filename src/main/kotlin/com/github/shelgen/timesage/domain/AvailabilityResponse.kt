package com.github.shelgen.timesage.domain

open class AvailabilityResponse(
    open val sessionLimit: Int?,
    open val dates: AvailabilityResponseDate
)

class MutableAvailabilityResponse(
    override var sessionLimit: Int?,
    override val dates: MutableAvailabilityResponseDate
) : AvailabilityResponse(
    sessionLimit = sessionLimit,
    dates = dates
) {
    constructor(availabilityResponse: AvailabilityResponse) : this(
        sessionLimit = availabilityResponse.sessionLimit,
        dates = MutableAvailabilityResponseDate(userResponses = availabilityResponse.dates)
    )

    companion object {
        fun createNew() = MutableAvailabilityResponse(
            sessionLimit = null,
            dates = MutableAvailabilityResponseDate(mutableMapOf())
        )
    }
}
