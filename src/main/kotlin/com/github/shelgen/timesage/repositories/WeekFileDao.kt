package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
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

    fun save(mondayDate: LocalDate, context: OperationContext, json: Json) {
        fileDao.save(getWeekFile(context, mondayDate), json)
    }

    fun loadOrInitialize(mondayDate: LocalDate, context: OperationContext): Json =
        fileDao.loadOrInitialize(getWeekFile(context, mondayDate))

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

    private fun getWeekFile(context: OperationContext, mondayDate: LocalDate): File =
        File(getWeeksDir(context), "$mondayDate.json")

    private fun getWeeksDir(context: OperationContext): File =
        File(getChannelDir(context), "weeks")
}
