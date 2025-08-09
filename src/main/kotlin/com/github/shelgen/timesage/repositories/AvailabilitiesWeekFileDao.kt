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
    ) {
        data class Response(
            val sessionLimit: Int?,
            val availability: Map<Instant, AvailabilityStatus>
        )

        enum class AvailabilityStatus {
            AVAILABLE, IF_NEED_BE, UNAVAILABLE
        }
    }

    private fun getWeekFile(startDate: LocalDate, context: OperationContext): File =
        File(getWeeksDir(context), "$startDate.json")

    private fun getWeeksDir(context: OperationContext): File =
        File(getChannelDir(context), "weeks")
}
