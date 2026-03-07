package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant
import java.io.File
import java.time.Instant
import java.time.LocalDate

class AvailabilitiesPeriodFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(period: DateRange, tenant: Tenant, json: Json) {
        fileDao.save(getPeriodFile(period, tenant), json)
    }

    fun load(period: DateRange, tenant: Tenant): Json? =
        fileDao.load(getPeriodFile(period, tenant))

    fun loadAll(tenant: Tenant): List<Json> {
        val periodsDir = getPeriodsDir(tenant)
        if (!periodsDir.exists()) return emptyList()
        return periodsDir.listFiles { f -> f.extension == "json" }
            .orEmpty()
            .mapNotNull { fileDao.load(it) }
    }

    data class Json(
        val messageId: Long?,
        val threadId: Long?,
        val headerMessageId: Long?,
        val sessionLimitAndUnavailableMessageId: Long?,
        val availabilityMessageIds: Map<String, Long> = emptyMap(),
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

    private fun getPeriodFile(dateRange: DateRange, tenant: Tenant): File =
        File(getPeriodsDir(tenant), "${dateRange.fromInclusive}_${dateRange.toInclusive}.json")

    private fun getPeriodsDir(tenant: Tenant): File =
        File(getTenantDir(tenant), "periods")
}
