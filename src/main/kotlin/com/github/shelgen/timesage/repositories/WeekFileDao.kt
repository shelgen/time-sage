package com.github.shelgen.timesage.repositories

import java.io.File
import java.time.LocalDate

class WeekFileDao {
    private val fileDao = CachedJsonFileDao<Json>(
        jsonClass = Json::class.java,
        initialContent = Json(
            weekAvailabilityMessageDiscordId = null,
            playerResponses = emptyMap()
        )
    )

    fun save(guildId: Long, mondayDate: LocalDate, json: Json) {
        fileDao.save(getWeekFile(guildId, mondayDate), json)
    }

    fun loadOrInitialize(guildId: Long, mondayDate: LocalDate): Json =
        fileDao.loadOrInitialize(getWeekFile(guildId, mondayDate))

    data class Json(
        val weekAvailabilityMessageDiscordId: Long?,
        val playerResponses: Map<Long, PlayerResponse>,
    ) {
        data class PlayerResponse(
            val sessionLimit: Int?,
            val availability: Map<LocalDate, AvailabilityStatus>
        )

        enum class AvailabilityStatus {
            AVAILABLE, IF_NEED_BE, UNAVAILABLE
        }
    }

    private fun getWeekFile(guildId: Long, mondayDate: LocalDate): File =
        File(getWeekDir(guildId), "$mondayDate.json")

    private fun getWeekDir(guildId: Long): File =
        File(getServerDir(guildId), "weeks")
}
