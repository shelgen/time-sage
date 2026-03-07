package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*
import java.time.LocalDate
import java.util.*

object AvailabilitiesPeriodRepository {
    private val dao = AvailabilitiesPeriodFileDao()

    fun loadOrInitialize(period: DateRange, tenant: Tenant): PlanningTargetPeriod =
        dao.load(period, tenant)?.toDomain() ?: PlanningTargetPeriod.DEFAULT

    @Synchronized
    fun <T> update(
        period: DateRange,
        tenant: Tenant,
        modification: (period: MutablePlanningTargetPeriod) -> T
    ): T {
        val existing = loadOrInitialize(period, tenant)
        val mutable = MutablePlanningTargetPeriod(existing)
        val returnValue = modification(mutable)
        dao.save(period, tenant, mutable.toJson())
        return returnValue
    }

    fun loadAll(tenant: Tenant): List<PlanningTargetPeriod> =
        dao.loadAll(tenant).map { it.toDomain() }

    private fun AvailabilitiesPeriodFileDao.Json.toDomain() = PlanningTargetPeriod(
        availabilityMessageOrThread = toAvailabilityMessageOrThread(),
        availabilityResponses = AvailabilityResponses(responses.map { (userId, r) -> userId to r.toDomain() }.toMap()),
        concluded = concluded,
        conclusionMessageId = conclusionMessageId,
        lastReminderDate = lastReminderDate,
    )

    private fun AvailabilitiesPeriodFileDao.Json.toAvailabilityMessageOrThread(): AvailabilityMessageOrThread? =
        when {
            threadId != null && headerMessageId != null -> AvailabilityMessageOrThread.AvailabilityThread(
                threadStartScreenMessageId = headerMessageId,
                threadChannelId = threadId,
                periodLevelScreenMessageId = sessionLimitAndUnavailableMessageId,
                availabilityWeekScreenMessageIds = availabilityMessageIds.mapKeys { (key, _) ->
                    val (from, to) = key.split("_")
                    DateRange(LocalDate.parse(from), LocalDate.parse(to))
                },
            )
            messageId != null -> AvailabilityMessageOrThread.AvailabilityMessage(messageId)
            else -> null
        }

    private fun AvailabilitiesPeriodFileDao.Json.Response.toDomain() = AvailabilityResponse(
        sessionLimit = sessionLimit,
        dates = AvailabilityResponseDate(availability.map { (ts, s) -> ts to s.toDomain() }.toMap())
    )

    private fun AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.toDomain() = when (this) {
        AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
        AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
        AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
    }

    private fun MutablePlanningTargetPeriod.toJson(): AvailabilitiesPeriodFileDao.Json {
        val thread = availabilityMessageOrThread as? AvailabilityMessageOrThread.AvailabilityThread
        val message = availabilityMessageOrThread as? AvailabilityMessageOrThread.AvailabilityMessage
        return AvailabilitiesPeriodFileDao.Json(
            messageId = message?.screenMessageId,
            threadId = thread?.threadChannelId,
            headerMessageId = thread?.threadStartScreenMessageId,
            sessionLimitAndUnavailableMessageId = thread?.periodLevelScreenMessageId,
            availabilityMessageIds = thread?.availabilityWeekScreenMessageIds
                ?.mapKeys { (period, _) -> "${period.fromInclusive}_${period.toInclusive}" }
                ?: emptyMap(),
            responses = availabilityResponses.map.map { (userId, r) -> userId to r.toJson() }.toMap(TreeMap()),
            concluded = concluded,
            conclusionMessageId = conclusionMessageId,
            lastReminderDate = lastReminderDate,
        )
    }

    private fun AvailabilityResponse.toJson() = AvailabilitiesPeriodFileDao.Json.Response(
        sessionLimit = sessionLimit,
        availability = dates.map.map { (ts, a) -> ts to a.toJson() }.toMap(TreeMap())
    )

    private fun AvailabilityStatus.toJson() = when (this) {
        AvailabilityStatus.AVAILABLE -> AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.AVAILABLE
        AvailabilityStatus.IF_NEED_BE -> AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.IF_NEED_BE
        AvailabilityStatus.UNAVAILABLE -> AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.UNAVAILABLE
    }
}
