package com.github.shelgen.timesage.repositories

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.time.DateRange
import java.io.File
import java.time.Instant

class PlanningProcessFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(dateRange: DateRange, tenant: Tenant, json: Json) {
        fileDao.save(getPlanningProcessFile(dateRange, tenant), json)
    }

    fun delete(dateRange: DateRange, tenant: Tenant) {
        fileDao.delete(getPlanningProcessFile(dateRange, tenant))
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
        val tenant: Tenant,
        val dateRange: String,
        val timeSlots: List<Instant>,
        val state: State,
        val availabilityInterface: AvailabilityInterface?,
        val availabilityResponses: Map<Long, AvailabilityResponse>,
        val sentReminders: List<SentReminder>,
        val conclusion: Conclusion?,
        val planAlternatives: List<Plan>?,
    ) {
        data class Tenant(
            val serverId: Long,
            val textChannelId: Long,
        )

        enum class State {
            PENDING, COLLECTING_AVAILABILITIES, LOCKED, CONCLUDED
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        @JsonSubTypes(
            JsonSubTypes.Type(value = AvailabilityMessage::class, name = "message"),
            JsonSubTypes.Type(value = AvailabilityThread::class, name = "thread"),
        )
        sealed interface AvailabilityInterface {
            val postedAt: Instant
        }

        data class AvailabilityMessage(
            override val postedAt: Instant,
            val messageId: Long,
        ) : AvailabilityInterface

        data class AvailabilityThread(
            override val postedAt: Instant,
            val threadStartMessageId: Long,
            val threadChannelId: Long,
            val periodLevelMessageId: Long,
            val chunkMessageIds: List<Long>,
        ) : AvailabilityInterface

        data class AvailabilityResponse(
            val sessionLimit: Int,
            val slotAvailabilities: Map<Instant, Availability>,
        )

        enum class Availability {
            AVAILABLE, IF_NEED_BE, UNAVAILABLE
        }

        data class SentReminder(
            val sentAt: Instant,
            val messageId: Long,
        )

        data class Conclusion(
            val messageId: Long,
            val planId: String,
            val scheduledEventIds: List<Long>,
        )

        data class Plan(
            val id: String,
            val sessions: List<Session>,
        )

        data class Session(
            val timeSlot: Instant,
            val activityId: Int,
            val participants: List<Participant>,
            val missingOptionalCount: Int,
        )

        data class Participant(
            val userId: Long,
            val ifNeedBe: Boolean,
        )
    }

    private fun getPlanningProcessFile(dateRange: DateRange, tenant: Tenant): File =
        File(getPeriodsDir(tenant), "${dateRange.fromInclusive}_${dateRange.toInclusive}.json")

    private fun getPeriodsDir(tenant: Tenant): File =
        File(getTenantDir(tenant), "periods")
}
