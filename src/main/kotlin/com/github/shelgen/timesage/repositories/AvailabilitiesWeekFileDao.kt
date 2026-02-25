package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
import java.io.File
import java.time.Instant
import java.time.LocalDate

class AvailabilitiesWeekFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(startDate: LocalDate, context: OperationContext, json: Json) {
        fileDao.save(getWeekFile(startDate, context), json)
    }

    fun load(startDate: LocalDate, context: OperationContext): Json? =
        fileDao.load(getWeekFile(startDate, context))

    data class Json(
        val availabilityMessageId: Long?,
        val responses: Map<Long, Response>,
        val concluded: Boolean = false,
        val conclusionMessageId: Long? = null,
    ) {
        data class Response(
            val sessionLimit: Int?,
            val availability: Map<Instant, AvailabilityStatus>
        )

        enum class AvailabilityStatus {
            AVAILABLE, IF_NEED_BE, UNAVAILABLE
        }
    }

    fun loadAll(context: OperationContext): List<Json> {
        val weeksDir = getWeeksDir(context)
        if (!weeksDir.exists()) return emptyList()
        return weeksDir.listFiles { f -> f.extension == "json" }
            .orEmpty()
            .mapNotNull { fileDao.load(it) }
    }

    private fun getWeekFile(startDate: LocalDate, context: OperationContext): File =
        File(getWeeksDir(context), "$startDate.json")

    private fun getWeeksDir(context: OperationContext): File =
        File(getChannelDir(context), "weeks")
}
