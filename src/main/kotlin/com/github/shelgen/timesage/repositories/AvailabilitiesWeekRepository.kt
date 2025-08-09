package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*
import java.time.LocalDate
import java.util.*

object AvailabilitiesWeekRepository {
    private val dao = AvailabilitiesWeekFileDao()

    fun loadOrInitialize(startDate: LocalDate, context: OperationContext): AvailabilitiesWeek =
        dao.load(startDate, context)
            ?.toDomain()
            ?: AvailabilitiesWeek.Companion.DEFAULT

    fun <T> update(
        startDate: LocalDate,
        context: OperationContext,
        modification: (week: MutableAvailabilitiesWeek) -> T
    ): T {
        val week = loadOrInitialize(startDate, context)
        val mutableWeek = MutableAvailabilitiesWeek(week)
        val returnValue = modification(mutableWeek)
        dao.save(startDate, context, mutableWeek.toJson())
        return returnValue
    }

    private fun AvailabilitiesWeekFileDao.Json.toDomain() = AvailabilitiesWeek(
        messageId = availabilityMessageId,
        responses = UserResponses(responses.map { (userId, response) -> userId to response.toDomain() }.toMap()),
    )

    private fun AvailabilitiesWeekFileDao.Json.Response.toDomain() = UserResponse(
        sessionLimit = sessionLimit,
        availabilities = DateAvailabilities(
            availability.map { (timeSlot, status) ->
                timeSlot to status.toDomain()
            }.toMap()
        )
    )

    private fun AvailabilitiesWeekFileDao.Json.AvailabilityStatus.toDomain() = when (this) {
        AvailabilitiesWeekFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
        AvailabilitiesWeekFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
        AvailabilitiesWeekFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
    }

    private fun MutableAvailabilitiesWeek.toJson() = AvailabilitiesWeekFileDao.Json(
        availabilityMessageId = messageId,
        responses = responses.map.map { (userId, response) -> userId to response.toJson() }.toMap(TreeMap())
    )

    private fun UserResponse.toJson() = AvailabilitiesWeekFileDao.Json.Response(
        sessionLimit = sessionLimit,
        availability = availabilities.map.map { (timeSlot, availability) ->
            timeSlot to availability.toJson()
        }.toMap(TreeMap())
    )

    private fun AvailabilityStatus.toJson() = when (this) {
        AvailabilityStatus.AVAILABLE -> AvailabilitiesWeekFileDao.Json.AvailabilityStatus.AVAILABLE
        AvailabilityStatus.IF_NEED_BE -> AvailabilitiesWeekFileDao.Json.AvailabilityStatus.IF_NEED_BE
        AvailabilityStatus.UNAVAILABLE -> AvailabilitiesWeekFileDao.Json.AvailabilityStatus.UNAVAILABLE
    }
}
