package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant
import java.io.File
import java.time.Instant
import java.time.LocalDate

class AvailabilitiesPeriodFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(targetPeriod: TargetPeriod, tenant: Tenant, json: Json) {
        fileDao.save(getPeriodFile(targetPeriod, tenant), json)
    }

    fun load(targetPeriod: TargetPeriod, tenant: Tenant): Json? =
        fileDao.load(getPeriodFile(targetPeriod, tenant))

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

    private fun getPeriodFile(targetPeriod: TargetPeriod, tenant: Tenant): File =
        File(getPeriodsDir(tenant), "${targetPeriod.fromInclusive}_${targetPeriod.toInclusive}.json")

    private fun getPeriodsDir(tenant: Tenant): File =
        File(getTenantDir(tenant), "periods")
}
