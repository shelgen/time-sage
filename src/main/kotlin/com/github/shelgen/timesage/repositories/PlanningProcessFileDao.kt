package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant
import java.io.File
import java.time.Instant
import java.time.LocalDate

class PlanningProcessFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(dateRange: DateRange, tenant: Tenant, json: Json) {
        fileDao.save(getPlanningProcessFile(dateRange, tenant), json)
    }

    fun load(dateRange: DateRange, tenant: Tenant): Json? =
        fileDao.load(getPlanningProcessFile(dateRange, tenant))

    fun loadAll(tenant: Tenant): List<Json> {
        val periodsDir = getPeriodsDir(tenant)
        if (!periodsDir.exists()) return emptyList()
        return periodsDir.listFiles { f -> f.extension == "json" }
            .orEmpty()
            .mapNotNull { fileDao.load(it) }
    }

    data class Json(
        val singleMessageId: Long?,
        val threadChannelId: Long?,
        val threadStartMessageId: Long?,
        val periodLevelMessageId: Long?,
        val weekMessageIds: Map<String, Long> = emptyMap(),
        val availabilityResponses: Map<Long, AvailabilityResponse>,
        val concluded: Boolean = false,
        val conclusionMessageId: Long? = null,
        val lastReminderDate: LocalDate? = null,
    ) {
        data class AvailabilityResponse(
            val sessionLimit: Int?,
            val availabilities: Map<Instant, AvailabilityStatus>
        )

        enum class AvailabilityStatus {
            AVAILABLE, IF_NEED_BE, UNAVAILABLE
        }
    }

    private fun getPlanningProcessFile(dateRange: DateRange, tenant: Tenant): File =
        File(getPeriodsDir(tenant), "${dateRange.fromInclusive}_${dateRange.toInclusive}.json")

    private fun getPeriodsDir(tenant: Tenant): File =
        File(getTenantDir(tenant), "periods")
}
