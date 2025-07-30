package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Week
import java.time.LocalDate

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
            weekAvailabilityMessageDiscordId = weekAvailabilityMessageDiscordId,
            playerResponses = playerResponses.map { (userId, response) ->
                userId to response.toPlayerResponse()
            }.toMap(),
        )

    private fun WeekFileDao.Json.PlayerResponse.toPlayerResponse(): Week.PlayerResponse =
        Week.PlayerResponse(
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
        val playerResponses: MutableMap<Long, PlayerResponse>,
    ) {
        constructor(week: Week) : this(
            guildId = week.guildId,
            mondayDate = week.mondayDate,
            weekAvailabilityMessageDiscordId = week.weekAvailabilityMessageDiscordId,
            playerResponses = week.playerResponses.map { (userId, response) ->
                userId to PlayerResponse(response)
            }.toMap().toMutableMap()
        )

        data class PlayerResponse(
            var sessionLimit: Int?,
            val availability: MutableMap<LocalDate, AvailabilityStatus>
        ) {
            constructor(playerResponse: Week.PlayerResponse) : this(
                sessionLimit = playerResponse.sessionLimit,
                availability = playerResponse.availability.toMutableMap()
            )
        }

        fun toJson(): WeekFileDao.Json =
            WeekFileDao.Json(
                weekAvailabilityMessageDiscordId = weekAvailabilityMessageDiscordId,
                playerResponses = playerResponses.map { (userId, response) -> userId to response.toJson() }.toMap()
            )

        private fun PlayerResponse.toJson(): WeekFileDao.Json.PlayerResponse =
            WeekFileDao.Json.PlayerResponse(
                sessionLimit = sessionLimit,
                availability = availability.map { (date, availability) -> date to availability.toJson() }.toMap()
            )

        private fun AvailabilityStatus.toJson(): WeekFileDao.Json.AvailabilityStatus =
            when (this) {
                AvailabilityStatus.AVAILABLE -> WeekFileDao.Json.AvailabilityStatus.AVAILABLE
                AvailabilityStatus.IF_NEED_BE -> WeekFileDao.Json.AvailabilityStatus.IF_NEED_BE
                AvailabilityStatus.UNAVAILABLE -> WeekFileDao.Json.AvailabilityStatus.UNAVAILABLE
            }
    }
}
