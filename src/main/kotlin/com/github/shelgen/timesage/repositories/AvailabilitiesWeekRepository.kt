package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.AvailabilitiesWeek
import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.OperationContext
import java.time.LocalDate
import java.util.*

object AvailabilitiesWeekRepository {
    private val dao = AvailabilitiesWeekFileDao()

    fun loadOrInitialize(startDate: LocalDate, context: OperationContext): AvailabilitiesWeek =
        dao.load(startDate, context)
            ?.toWeek()
            ?: AvailabilitiesWeek.Companion.DEFAULT

    fun <T> update(
        startDate: LocalDate,
        context: OperationContext,
        modification: (week: MutableWeek) -> T
    ): T {
        val week = loadOrInitialize(startDate, context)
        val mutableWeek = MutableWeek(week)
        val returnValue = modification(mutableWeek)
        dao.save(startDate, context, mutableWeek.toJson())
        return returnValue
    }

    private fun AvailabilitiesWeekFileDao.Json.toWeek(): AvailabilitiesWeek =
        AvailabilitiesWeek(
            messageId = availabilityMessageId,
            responses = responses.map { (userId, response) -> userId to response.toResponse() }.toMap(),
        )

    private fun AvailabilitiesWeekFileDao.Json.Response.toResponse(): AvailabilitiesWeek.Response =
        AvailabilitiesWeek.Response(
            sessionLimit = sessionLimit,
            availabilities = availability.map { (date, status) -> date to status.toAvailabilityStatus() }.toMap()
        )

    private fun AvailabilitiesWeekFileDao.Json.AvailabilityStatus.toAvailabilityStatus(): AvailabilityStatus =
        when (this) {
            AvailabilitiesWeekFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
            AvailabilitiesWeekFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
            AvailabilitiesWeekFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
        }

    data class MutableWeek(
        var messageDiscordId: Long?,
        val responses: MutableMap<Long, Response>,
    ) {
        constructor(week: AvailabilitiesWeek) : this(
            messageDiscordId = week.messageId,
            responses = week.responses.map { (userId, response) -> userId to Response(response) }.toMap().toMutableMap()
        )

        data class Response(
            var sessionLimit: Int?,
            val availability: MutableMap<LocalDate, AvailabilityStatus>
        ) {
            constructor(response: AvailabilitiesWeek.Response) : this(
                sessionLimit = response.sessionLimit,
                availability = response.availabilities.toMutableMap()
            )
        }

        fun toJson(): AvailabilitiesWeekFileDao.Json =
            AvailabilitiesWeekFileDao.Json(
                availabilityMessageId = messageDiscordId,
                responses = responses.map { (userId, response) -> userId to response.toJson() }.toMap(TreeMap())
            )

        private fun Response.toJson(): AvailabilitiesWeekFileDao.Json.Response =
            AvailabilitiesWeekFileDao.Json.Response(
                sessionLimit = sessionLimit,
                availability = availability.map { (date, availability) -> date to availability.toJson() }
                    .toMap(TreeMap())
            )

        private fun AvailabilityStatus.toJson(): AvailabilitiesWeekFileDao.Json.AvailabilityStatus =
            when (this) {
                AvailabilityStatus.AVAILABLE -> AvailabilitiesWeekFileDao.Json.AvailabilityStatus.AVAILABLE
                AvailabilityStatus.IF_NEED_BE -> AvailabilitiesWeekFileDao.Json.AvailabilityStatus.IF_NEED_BE
                AvailabilityStatus.UNAVAILABLE -> AvailabilitiesWeekFileDao.Json.AvailabilityStatus.UNAVAILABLE
            }
    }
}
