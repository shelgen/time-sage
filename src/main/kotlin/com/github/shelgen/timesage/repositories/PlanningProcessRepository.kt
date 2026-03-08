package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.discord.DiscordThreadChannelId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.planning.Availability
import com.github.shelgen.timesage.planning.AvailabilityInterface
import com.github.shelgen.timesage.planning.AvailabilityMessage
import com.github.shelgen.timesage.planning.AvailabilityResponse
import com.github.shelgen.timesage.planning.AvailabilityThread
import com.github.shelgen.timesage.planning.Conclusion
import com.github.shelgen.timesage.planning.MutablePlanningProcess
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.planning.SentReminder
import com.github.shelgen.timesage.time.DateRange
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
        planningProcess: PlanningProcess,
        modification: (planningProcess: MutablePlanningProcess) -> T
    ): T {
        val mutable = MutablePlanningProcess(planningProcess)
        val returnValue = modification(mutable)
        dao.save(planningProcess.dateRange, planningProcess.tenant, mutable.toJson())
        return returnValue
    }

    fun loadAll(tenant: Tenant): List<PlanningProcess> =
        dao.loadAll(tenant).map { it.toDomain() }

    private fun PlanningProcessFileDao.Json.toDomain() = PlanningProcess(
        tenant = tenant.toDomain(),
        dateRange = DateRange.deserialize(dateRange),
        state = state.toDomain(),
        availabilityInterface = availabilityInterface.toDomain(),
        availabilityResponses = availabilityResponses
            .map { (userId, response) -> DiscordUserId(userId) to response.toDomain() }
            .toMap(),
        sentReminders = sentReminders.map { it.toDomain() },
        conclusion = conclusion?.toDomain(),
    )

    private fun PlanningProcessFileDao.Json.Tenant.toDomain() =
        Tenant(
            server = DiscordServerId(serverId),
            textChannel = DiscordTextChannelId(textChannelId),
        )

    private fun PlanningProcessFileDao.Json.State.toDomain() = when (this) {
        PlanningProcessFileDao.Json.State.COLLECTING_AVAILABILITIES -> PlanningProcess.State.COLLECTING_AVAILABILITIES
        PlanningProcessFileDao.Json.State.LOCKED -> PlanningProcess.State.LOCKED
        PlanningProcessFileDao.Json.State.CONCLUDED -> PlanningProcess.State.CONCLUDED
    }

    private fun PlanningProcessFileDao.Json.AvailabilityInterface.toDomain(): AvailabilityInterface = when (this) {
        is PlanningProcessFileDao.Json.AvailabilityMessage -> AvailabilityMessage(
            postedAt = postedAt,
            message = DiscordMessageId(messageId),
        )

        is PlanningProcessFileDao.Json.AvailabilityThread -> AvailabilityThread(
            postedAt = postedAt,
            threadStartMessage = DiscordMessageId(threadStartMessageId),
            threadChannel = DiscordThreadChannelId(threadChannelId),
            periodLevelMessage = DiscordMessageId(periodLevelMessageId),
            weekMessages = weekMessageIds.map { (dateRangeString, messageId) ->
                DateRange.deserialize(dateRangeString) to DiscordMessageId(messageId)
            }.toMap(),
        )
    }

    private fun PlanningProcessFileDao.Json.AvailabilityResponse.toDomain() =
        AvailabilityResponse(
            sessionLimit = sessionLimit,
            slotAvailabilities = slotAvailabilities
                .map { (timeSlot, availability) -> timeSlot to availability.toDomain() }
                .toMap(),
        )

    private fun PlanningProcessFileDao.Json.Availability.toDomain() = when (this) {
        PlanningProcessFileDao.Json.Availability.AVAILABLE -> Availability.AVAILABLE
        PlanningProcessFileDao.Json.Availability.IF_NEED_BE -> Availability.IF_NEED_BE
        PlanningProcessFileDao.Json.Availability.UNAVAILABLE -> Availability.UNAVAILABLE
    }

    private fun PlanningProcessFileDao.Json.SentReminder.toDomain() = SentReminder(
        sentAt = sentAt,
        message = DiscordMessageId(messageId),
    )

    private fun PlanningProcessFileDao.Json.Conclusion.toDomain() = Conclusion(
        message = DiscordMessageId(messageId),
        plan = PlanId(UUID.fromString(planId)),
    )

    private fun PlanningProcess.toJson() = PlanningProcessFileDao.Json(
        tenant = tenant.toJson(),
        dateRange = dateRange.serialize(),
        state = state.toJson(),
        availabilityInterface = availabilityInterface.toJson(),
        availabilityResponses = availabilityResponses
            .map { (user, response) -> user.id to response.toJson() }
            .toMap(TreeMap()),
        sentReminders = sentReminders.map { it.toJson() },
        conclusion = conclusion?.toJson(),
    )

    private fun Tenant.toJson() =
        PlanningProcessFileDao.Json.Tenant(
            serverId = server.id,
            textChannelId = textChannel.id,
        )

    private fun PlanningProcess.State.toJson() = when (this) {
        PlanningProcess.State.COLLECTING_AVAILABILITIES -> PlanningProcessFileDao.Json.State.COLLECTING_AVAILABILITIES
        PlanningProcess.State.LOCKED -> PlanningProcessFileDao.Json.State.LOCKED
        PlanningProcess.State.CONCLUDED -> PlanningProcessFileDao.Json.State.CONCLUDED
    }

    private fun AvailabilityInterface.toJson(): PlanningProcessFileDao.Json.AvailabilityInterface = when (this) {
        is AvailabilityMessage -> PlanningProcessFileDao.Json.AvailabilityMessage(
            postedAt = postedAt,
            messageId = message.id,
        )

        is AvailabilityThread -> PlanningProcessFileDao.Json.AvailabilityThread(
            postedAt = postedAt,
            threadStartMessageId = threadStartMessage.id,
            threadChannelId = threadChannel.id,
            periodLevelMessageId = periodLevelMessage.id,
            weekMessageIds = weekMessages
                .map { (dateRange, message) -> dateRange.serialize() to message.id }
                .toMap(TreeMap()),
        )
    }

    private fun AvailabilityResponse.toJson() = PlanningProcessFileDao.Json.AvailabilityResponse(
        sessionLimit = sessionLimit,
        slotAvailabilities = slotAvailabilities
            .map { (timeSlot, availability) -> timeSlot to availability.toJson() }
            .toMap(TreeMap()),
    )

    private fun Availability.toJson() = when (this) {
        Availability.AVAILABLE -> PlanningProcessFileDao.Json.Availability.AVAILABLE
        Availability.IF_NEED_BE -> PlanningProcessFileDao.Json.Availability.IF_NEED_BE
        Availability.UNAVAILABLE -> PlanningProcessFileDao.Json.Availability.UNAVAILABLE
    }

    private fun SentReminder.toJson() = PlanningProcessFileDao.Json.SentReminder(
        sentAt = sentAt,
        messageId = message.id,
    )

    private fun Conclusion.toJson() = PlanningProcessFileDao.Json.Conclusion(
        messageId = message.id,
        planId = plan.value.toString(),
    )
}
