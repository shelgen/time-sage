package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.planning.AvailabilityMessage
import com.github.shelgen.timesage.planning.AvailabilityResponse
import com.github.shelgen.timesage.domain.AvailabilityResponseDate
import com.github.shelgen.timesage.planning.AvailabilityResponses
import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.planning.MutablePlanningProcess
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.Tenant
import java.time.LocalDate
import java.util.*

object PlanningProcessRepository {
    private val dao = PlanningProcessFileDao()

    fun load(dateRange: DateRange, tenant: Tenant): PlanningProcess? =
        dao.load(dateRange, tenant)?.toDomain()

    fun saveNew(planningProcess: PlanningProcess) {
        dao.save(planningProcess.dateRange, planningProcess.tenant, planningProcess.toJson())
    }

    @Synchronized
    fun <T> update(
        dateRange: DateRange,
        tenant: Tenant,
        modification: (planningProcess: MutablePlanningProcess) -> T
    ): T {
        val existing = loadOrInitialize(dateRange, tenant)
        val mutable = MutablePlanningProcess(existing)
        val returnValue = modification(mutable)
        dao.save(dateRange, tenant, mutable.toJson())
        return returnValue
    }

    fun loadAll(tenant: Tenant): List<PlanningProcess> =
        dao.loadAll(tenant).map { it.toDomain() }

    private fun PlanningProcessFileDao.Json.toDomain() = PlanningProcess(
        availabilityMessage = toAvailabilityMessage(),
        availabilityResponses = AvailabilityResponses(availabilityResponses.map { (userId, r) -> userId to r.toDomain() }.toMap()),
        conclusionMessageId = conclusionMessageId,
        lastReminderDate = lastReminderDate,
    )

    private fun PlanningProcessFileDao.Json.toAvailabilityMessage(): AvailabilityMessage? =
        when {
            threadChannelId != null && threadStartMessageId != null -> AvailabilityMessage.Thread(
                threadStartScreenMessageId = threadStartMessageId,
                threadChannelId = threadChannelId,
                periodLevelScreenMessageId = periodLevelMessageId,
                availabilityWeekScreenMessageIds = weekMessageIds.mapKeys { (key, _) ->
                    val (from, to) = key.split("_")
                    DateRange(LocalDate.parse(from), LocalDate.parse(to))
                },
            )
            singleMessageId != null -> AvailabilityMessage.SingleMessage(singleMessageId)
            else -> null
        }

    private fun PlanningProcessFileDao.Json.AvailabilityResponse.toDomain() = AvailabilityResponse(
        sessionLimit = sessionLimit,
        dates = AvailabilityResponseDate(availabilities.map { (ts, s) -> ts to s.toDomain() }.toMap())
    )

    private fun PlanningProcessFileDao.Json.AvailabilityStatus.toDomain() = when (this) {
        PlanningProcessFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
        PlanningProcessFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
        PlanningProcessFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
    }

    private fun PlanningProcess.toJson(): PlanningProcessFileDao.Json {
        val thread = availabilityMessage as? AvailabilityMessage.Thread
        val singleMessage = availabilityMessage as? AvailabilityMessage.SingleMessage
        return PlanningProcessFileDao.Json(
            singleMessageId = singleMessage?.messageId,
            threadChannelId = thread?.threadChannelId,
            threadStartMessageId = thread?.threadStartScreenMessageId,
            periodLevelMessageId = thread?.periodLevelScreenMessageId,
            weekMessageIds = thread?.availabilityWeekScreenMessageIds
                ?.mapKeys { (period, _) -> "${period.fromInclusive}_${period.toInclusive}" }
                ?: emptyMap(),
            availabilityResponses = availabilityResponses.map.map { (userId, r) -> userId to r.toJson() }.toMap(TreeMap()),
            concluded = concluded,
            conclusionMessageId = conclusionMessageId,
            lastReminderDate = lastReminderDate,
        )
    }

    private fun AvailabilityResponse.toJson() = PlanningProcessFileDao.Json.AvailabilityResponse(
        sessionLimit = sessionLimit,
        availabilities = dates.map.map { (ts, a) -> ts to a.toJson() }.toMap(TreeMap())
    )

    private fun AvailabilityStatus.toJson() = when (this) {
        AvailabilityStatus.AVAILABLE -> PlanningProcessFileDao.Json.AvailabilityStatus.AVAILABLE
        AvailabilityStatus.IF_NEED_BE -> PlanningProcessFileDao.Json.AvailabilityStatus.IF_NEED_BE
        AvailabilityStatus.UNAVAILABLE -> PlanningProcessFileDao.Json.AvailabilityStatus.UNAVAILABLE
    }
}
