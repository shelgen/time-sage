package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*
import java.time.YearMonth
import java.util.*

object AvailabilitiesMonthRepository {
    private val dao = AvailabilitiesMonthFileDao()

    fun loadOrInitialize(yearMonth: YearMonth, context: OperationContext): AvailabilitiesMonth =
        dao.load(yearMonth, context)
            ?.toDomain()
            ?: AvailabilitiesMonth.DEFAULT

    @Synchronized
    fun <T> update(
        yearMonth: YearMonth,
        context: OperationContext,
        modification: (month: MutableAvailabilitiesMonth) -> T
    ): T {
        val month = loadOrInitialize(yearMonth, context)
        val mutableMonth = MutableAvailabilitiesMonth(month)
        val returnValue = modification(mutableMonth)
        dao.save(yearMonth, context, mutableMonth.toJson())
        return returnValue
    }

    private fun AvailabilitiesMonthFileDao.Json.toDomain() = AvailabilitiesMonth(
        headerMessageId = headerMessageId,
        threadId = threadId,
        responses = UserResponses(responses.map { (userId, response) -> userId to response.toDomain() }.toMap()),
        concluded = concluded,
        conclusionMessageId = conclusionMessageId,
        lastReminderDate = lastReminderDate,
    )

    private fun AvailabilitiesMonthFileDao.Json.Response.toDomain() = UserResponse(
        sessionLimit = sessionLimit,
        availabilities = DateAvailabilities(
            availability.map { (timeSlot, status) ->
                timeSlot to status.toDomain()
            }.toMap()
        )
    )

    private fun AvailabilitiesMonthFileDao.Json.AvailabilityStatus.toDomain() = when (this) {
        AvailabilitiesMonthFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
        AvailabilitiesMonthFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
        AvailabilitiesMonthFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
    }

    private fun MutableAvailabilitiesMonth.toJson() = AvailabilitiesMonthFileDao.Json(
        headerMessageId = headerMessageId,
        threadId = threadId,
        responses = responses.map.map { (userId, response) -> userId to response.toJson() }.toMap(TreeMap()),
        concluded = concluded,
        conclusionMessageId = conclusionMessageId,
        lastReminderDate = lastReminderDate,
    )

    private fun UserResponse.toJson() = AvailabilitiesMonthFileDao.Json.Response(
        sessionLimit = sessionLimit,
        availability = availabilities.map.map { (timeSlot, availability) ->
            timeSlot to availability.toJson()
        }.toMap(TreeMap())
    )

    private fun AvailabilityStatus.toJson() = when (this) {
        AvailabilityStatus.AVAILABLE -> AvailabilitiesMonthFileDao.Json.AvailabilityStatus.AVAILABLE
        AvailabilityStatus.IF_NEED_BE -> AvailabilitiesMonthFileDao.Json.AvailabilityStatus.IF_NEED_BE
        AvailabilityStatus.UNAVAILABLE -> AvailabilitiesMonthFileDao.Json.AvailabilityStatus.UNAVAILABLE
    }
}
