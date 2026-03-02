package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import java.io.File
import java.time.Instant
import java.time.LocalDate

class AvailabilitiesPeriodFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(period: DatePeriod, context: OperationContext, json: Json) {
        fileDao.save(getPeriodFile(period, context), json)
    }

    fun load(period: DatePeriod, context: OperationContext): Json? =
        fileDao.load(getPeriodFile(period, context))

    fun loadAll(context: OperationContext): List<Json> {
        val periodsDir = getPeriodsDir(context)
        if (!periodsDir.exists()) return emptyList()
        return periodsDir.listFiles { f -> f.extension == "json" }
            .orEmpty()
            .mapNotNull { fileDao.load(it) }
    }

    data class Json(
        val messageId: Long?,
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

    private fun getPeriodFile(period: DatePeriod, context: OperationContext): File =
        File(getPeriodsDir(context), "${period.fromDate}_${period.toDate}.json")

    private fun getPeriodsDir(context: OperationContext): File =
        File(getChannelDir(context), "periods")
}
