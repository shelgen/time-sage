package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*
import java.time.LocalDate
import java.util.*

object AvailabilitiesPeriodRepository {
    private val dao = AvailabilitiesPeriodFileDao()

    fun loadOrInitialize(period: DatePeriod, context: OperationContext): AvailabilitiesPeriod =
        dao.load(period, context)?.toDomain() ?: AvailabilitiesPeriod.DEFAULT

    @Synchronized
    fun <T> update(
        period: DatePeriod,
        context: OperationContext,
        modification: (period: MutableAvailabilitiesPeriod) -> T
    ): T {
        val existing = loadOrInitialize(period, context)
        val mutable = MutableAvailabilitiesPeriod(existing)
        val returnValue = modification(mutable)
        dao.save(period, context, mutable.toJson())
        return returnValue
    }

    fun loadAll(context: OperationContext): List<AvailabilitiesPeriod> =
        dao.loadAll(context).map { it.toDomain() }

    private fun AvailabilitiesPeriodFileDao.Json.toDomain() = AvailabilitiesPeriod(
        availabilityMessageOrThread = toAvailabilityMessageOrThread(),
        responses = UserResponses(responses.map { (userId, r) -> userId to r.toDomain() }.toMap()),
        concluded = concluded,
        conclusionMessageId = conclusionMessageId,
        lastReminderDate = lastReminderDate,
    )

    private fun AvailabilitiesPeriodFileDao.Json.toAvailabilityMessageOrThread(): AvailabilityMessageOrThread? =
        when {
            threadId != null && headerMessageId != null -> AvailabilityMessageOrThread.AvailabilityThread(
                headerMessageId = headerMessageId,
                threadId = threadId,
                sessionLimitAndUnavailableMessageId = sessionLimitAndUnavailableMessageId,
                availabilityMessageIds = availabilityMessageIds.mapKeys { (key, _) ->
                    val (from, to) = key.split("_")
                    DatePeriod(LocalDate.parse(from), LocalDate.parse(to))
                },
            )
            messageId != null -> AvailabilityMessageOrThread.AvailabilityMessage(messageId)
            else -> null
        }

    private fun AvailabilitiesPeriodFileDao.Json.Response.toDomain() = UserResponse(
        sessionLimit = sessionLimit,
        availabilities = DateAvailabilities(availability.map { (ts, s) -> ts to s.toDomain() }.toMap())
    )

    private fun AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.toDomain() = when (this) {
        AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.AVAILABLE -> AvailabilityStatus.AVAILABLE
        AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.IF_NEED_BE
        AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.UNAVAILABLE
    }

    private fun MutableAvailabilitiesPeriod.toJson(): AvailabilitiesPeriodFileDao.Json {
        val thread = availabilityMessageOrThread as? AvailabilityMessageOrThread.AvailabilityThread
        val message = availabilityMessageOrThread as? AvailabilityMessageOrThread.AvailabilityMessage
        return AvailabilitiesPeriodFileDao.Json(
            messageId = message?.messageId,
            threadId = thread?.threadId,
            headerMessageId = thread?.headerMessageId,
            sessionLimitAndUnavailableMessageId = thread?.sessionLimitAndUnavailableMessageId,
            availabilityMessageIds = thread?.availabilityMessageIds
                ?.mapKeys { (period, _) -> "${period.fromDate}_${period.toDate}" }
                ?: emptyMap(),
            responses = responses.map.map { (userId, r) -> userId to r.toJson() }.toMap(TreeMap()),
            concluded = concluded,
            conclusionMessageId = conclusionMessageId,
            lastReminderDate = lastReminderDate,
        )
    }

    private fun UserResponse.toJson() = AvailabilitiesPeriodFileDao.Json.Response(
        sessionLimit = sessionLimit,
        availability = availabilities.map.map { (ts, a) -> ts to a.toJson() }.toMap(TreeMap())
    )

    private fun AvailabilityStatus.toJson() = when (this) {
        AvailabilityStatus.AVAILABLE -> AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.AVAILABLE
        AvailabilityStatus.IF_NEED_BE -> AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.IF_NEED_BE
        AvailabilityStatus.UNAVAILABLE -> AvailabilitiesPeriodFileDao.Json.AvailabilityStatus.UNAVAILABLE
    }
}
