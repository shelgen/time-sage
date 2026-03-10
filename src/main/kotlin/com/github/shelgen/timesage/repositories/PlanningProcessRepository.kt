package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordScheduledEventId
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.discord.DiscordThreadChannelId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.plan.Participant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.plan.Session
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

    fun delete(planningProcess: PlanningProcess) {
        dao.delete(planningProcess.dateRange, planningProcess.tenant)
    }

    private fun PlanningProcessFileDao.Json.toDomain() = PlanningProcess(
        tenant = tenant.toDomain(),
        dateRange = DateRange.deserialize(dateRange),
        timeSlots = timeSlots,
        state = state.toDomain(),
        availabilityInterface = availabilityInterface?.toDomain(),
        availabilityResponses = availabilityResponses
            .map { (userId, response) -> DiscordUserId(userId) to response.toDomain() }
            .toMap(),
        sentReminders = sentReminders.map { it.toDomain() },
        conclusion = conclusion?.toDomain(),
        planAlternatives = planAlternatives?.map { it.toDomain() } ?: emptyList(),
    )

    private fun PlanningProcessFileDao.Json.Tenant.toDomain() =
        Tenant(
            server = DiscordServerId(serverId),
            textChannel = DiscordTextChannelId(textChannelId),
        )

    private fun PlanningProcessFileDao.Json.State.toDomain() = when (this) {
        PlanningProcessFileDao.Json.State.PENDING -> PlanningProcess.State.PENDING
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
            timeSlotChunks = timeSlotChunks.map {
                AvailabilityThread.TimeSlotChunk(
                    size = it.size,
                    message = DiscordMessageId(it.messageId)
                )
            },
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
        scheduledEvents = scheduledEventIds.map { DiscordScheduledEventId(it) },
    )

    private fun PlanningProcess.toJson() = PlanningProcessFileDao.Json(
        tenant = tenant.toJson(),
        dateRange = dateRange.serialize(),
        timeSlots = timeSlots,
        state = state.toJson(),
        availabilityInterface = availabilityInterface?.toJson(),
        availabilityResponses = availabilityResponses
            .map { (user, response) -> user.id to response.toJson() }
            .toMap(TreeMap()),
        sentReminders = sentReminders.map { it.toJson() },
        conclusion = conclusion?.toJson(),
        planAlternatives = planAlternatives.map { it.toJson() },
    )

    private fun Tenant.toJson() =
        PlanningProcessFileDao.Json.Tenant(
            serverId = server.id,
            textChannelId = textChannel.id,
        )

    private fun PlanningProcess.State.toJson() = when (this) {
        PlanningProcess.State.PENDING -> PlanningProcessFileDao.Json.State.PENDING
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
            timeSlotChunks = timeSlotChunks.map {
                PlanningProcessFileDao.Json.AvailabilityThread.TimeSlotChunk(
                    size = it.size,
                    messageId = it.message.id
                )
            },
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
        scheduledEventIds = scheduledEvents.map { it.id },
    )

    private fun PlanningProcessFileDao.Json.Plan.toDomain() = Plan(
        id = PlanId(UUID.fromString(id)),
        sessions = sessions.map { it.toDomain() },
    )

    private fun PlanningProcessFileDao.Json.Session.toDomain() = Session(
        timeSlot = timeSlot,
        activityId = ActivityId(activityId),
        participants = participants.map { it.toDomain() }.toSet(),
        missingOptionalCount = missingOptionalCount,
    )

    private fun PlanningProcessFileDao.Json.Participant.toDomain() = Participant(
        user = DiscordUserId(userId),
        ifNeedBe = ifNeedBe,
    )

    private fun Plan.toJson() = PlanningProcessFileDao.Json.Plan(
        id = id.value.toString(),
        sessions = sessions.map { it.toJson() },
    )

    private fun Session.toJson() = PlanningProcessFileDao.Json.Session(
        timeSlot = timeSlot,
        activityId = activityId.value,
        participants = participants.map { it.toJson() },
        missingOptionalCount = missingOptionalCount,
    )

    private fun Participant.toJson() = PlanningProcessFileDao.Json.Participant(
        userId = user.id,
        ifNeedBe = ifNeedBe,
    )
}
