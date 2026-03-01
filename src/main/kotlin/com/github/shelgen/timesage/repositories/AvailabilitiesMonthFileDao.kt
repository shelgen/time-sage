package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class AvailabilitiesMonthFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(yearMonth: YearMonth, context: OperationContext, json: Json) {
        fileDao.save(getMonthFile(yearMonth, context), json)
    }

    fun load(yearMonth: YearMonth, context: OperationContext): Json? =
        fileDao.load(getMonthFile(yearMonth, context))

    data class Json(
        val headerMessageId: Long?,
        val threadId: Long?,
        val responses: Map<Long, Response>,
        val concluded: Boolean = false,
        val conclusionMessageId: Long? = null,
        val lastReminderDate: LocalDate? = null,
    ) {
        data class Response(
            val sessionLimit: Int?,
            val availability: Map<Instant, AvailabilityStatus>
        )

        enum class AvailabilityStatus {
            AVAILABLE, IF_NEED_BE, UNAVAILABLE
        }
    }

    private fun getMonthFile(yearMonth: YearMonth, context: OperationContext): File =
        File(getMonthsDir(context), "$yearMonth.json")

    private fun getMonthsDir(context: OperationContext): File =
        File(getChannelDir(context), "months")
}
