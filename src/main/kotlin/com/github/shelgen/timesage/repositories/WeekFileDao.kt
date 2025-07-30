package com.github.shelgen.timesage.repositories

import java.io.File
import java.time.LocalDate

class WeekFileDao {
    private val fileDao = CachedJsonFileDao<Json>(
        jsonClass = Json::class.java,
        initialContent = Json(
            availabilityMessageId = null,
            responses = emptyMap()
        )
    )

    fun save(guildId: Long, mondayDate: LocalDate, json: Json) {
        fileDao.save(getWeekFile(guildId, mondayDate), json)
    }

    fun loadOrInitialize(guildId: Long, mondayDate: LocalDate): Json =
        fileDao.loadOrInitialize(getWeekFile(guildId, mondayDate))

    data class Json(
        val availabilityMessageId: Long?,
        val responses: Map<Long, Response>,
    ) {
        data class Response(
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
