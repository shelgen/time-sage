package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Week
import java.time.LocalDate
import java.util.TreeMap

object WeekRepository {
    private val dao = WeekFileDao()

    fun loadOrInitialize(guildId: Long, mondayDate: LocalDate): Week =
        dao.loadOrInitialize(guildId, mondayDate).toWeek(guildId, mondayDate)

    fun <T> update(
        guildId: Long,
        mondayDate: LocalDate,
        modification: (week: MutableWeek) -> T
    ): T {
        val week = loadOrInitialize(guildId, mondayDate)
        val mutableWeek = MutableWeek(week)
        val returnValue = modification(mutableWeek)
        dao.save(guildId, mondayDate, mutableWeek.toJson())
        return returnValue
    }

    private fun WeekFileDao.Json.toWeek(guildId: Long, mondayDate: LocalDate): Week =
        Week(
            guildId = guildId,
            mondayDate = mondayDate,
            weekAvailabilityMessageDiscordId = availabilityMessageId,
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
        val guildId: Long,
        val mondayDate: LocalDate,
        var weekAvailabilityMessageDiscordId: Long?,
        val responses: MutableMap<Long, Response>,
    ) {
        constructor(week: Week) : this(
            guildId = week.guildId,
            mondayDate = week.mondayDate,
            weekAvailabilityMessageDiscordId = week.weekAvailabilityMessageDiscordId,
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
                availabilityMessageId = weekAvailabilityMessageDiscordId,
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
