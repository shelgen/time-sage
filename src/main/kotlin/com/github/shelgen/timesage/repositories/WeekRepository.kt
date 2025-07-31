package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Week
import java.time.LocalDate
import java.util.TreeMap

object WeekRepository {
    private val dao = WeekFileDao()

    fun loadOrInitialize(mondayDate: LocalDate, context: OperationContext): Week =
        dao.loadOrInitialize(mondayDate, context).toWeek(mondayDate)

    fun <T> update(
        mondayDate: LocalDate,
        context: OperationContext,
        modification: (week: MutableWeek) -> T
    ): T {
        val week = loadOrInitialize(mondayDate, context)
        val mutableWeek = MutableWeek(week)
        val returnValue = modification(mutableWeek)
        dao.save(mondayDate, context, mutableWeek.toJson())
        return returnValue
    }

    private fun WeekFileDao.Json.toWeek(mondayDate: LocalDate): Week =
        Week(
            mondayDate = mondayDate,
            messageDiscordId = availabilityMessageId,
            responses = responses.map { (userId, response) ->
                userId to response.toResponse()
            }.toMap(),
        )

    private fun WeekFileDao.Json.Response.toResponse(): Week.Response =
        Week.Response(
            sessionLimit = sessionLimit,
            availability = availability.map { (date, status) -> date to status.toAvailabilityStatus() }.toMap()
        )

    private fun WeekFileDao.Json.AvailabilityStatus.toAvailabilityStatus(): AvailabilityStatus =
        when (this) {
            WeekFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
            WeekFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
            WeekFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
        }

    data class MutableWeek(
        val mondayDate: LocalDate,
        var messageDiscordId: Long?,
        val responses: MutableMap<Long, Response>,
    ) {
        constructor(week: Week) : this(
            mondayDate = week.mondayDate,
            messageDiscordId = week.messageDiscordId,
            responses = week.responses.map { (userId, response) ->
                userId to Response(response)
            }.toMap().toMutableMap()
        )

        data class Response(
            var sessionLimit: Int?,
            val availability: MutableMap<LocalDate, AvailabilityStatus>
        ) {
            constructor(response: Week.Response) : this(
                sessionLimit = response.sessionLimit,
                availability = response.availability.toMutableMap()
            )
        }

        fun toJson(): WeekFileDao.Json =
            WeekFileDao.Json(
                availabilityMessageId = messageDiscordId,
                responses = responses.map { (userId, response) -> userId to response.toJson() }.toMap(TreeMap())
            )

        private fun Response.toJson(): WeekFileDao.Json.Response =
            WeekFileDao.Json.Response(
                sessionLimit = sessionLimit,
                availability = availability.map { (date, availability) -> date to availability.toJson() }.toMap(TreeMap())
            )

        private fun AvailabilityStatus.toJson(): WeekFileDao.Json.AvailabilityStatus =
            when (this) {
                AvailabilityStatus.AVAILABLE -> WeekFileDao.Json.AvailabilityStatus.AVAILABLE
                AvailabilityStatus.IF_NEED_BE -> WeekFileDao.Json.AvailabilityStatus.IF_NEED_BE
                AvailabilityStatus.UNAVAILABLE -> WeekFileDao.Json.AvailabilityStatus.UNAVAILABLE
            }
    }
}
