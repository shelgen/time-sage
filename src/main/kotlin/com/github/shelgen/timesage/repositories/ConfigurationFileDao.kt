package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

class ConfigurationFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(context: OperationContext, json: Json) {
        fileDao.save(getConfigurationFile(context), json)
    }

    fun load(context: OperationContext): Json? =
        fileDao.load(getConfigurationFile(context))

    fun findAllOperationContexts(): List<OperationContext> =
        findAllGuildIds().flatMap { guildId ->
            findAllChannelIds(guildId).map { channelId ->
                OperationContext(guildId = guildId, channelId = channelId)
            }
        }.filter { context -> getConfigurationFile(context).exists() }

    private fun getConfigurationFile(context: OperationContext): File =
        File(getChannelDir(context), CONFIGURATION_FILE_NAME)

    data class Json(
        val enabled: Boolean,
        val timeZone: TimeZone?,
        val scheduling: Scheduling,
        val activities: List<Activity>,
        val voiceChannelId: Long? = null
    ) {
        data class Activity(
            val id: Int,
            val name: String,
            val participants: Participants,
            val maxMissingOptionalParticipants: Int
        )

        data class Participants(
            val required: Set<Long>,
            val optional: Set<Long>
        )

        data class Scheduling(
            val type: SchedulingType,
            val startDayOfWeek: DayOfWeek,
            val timeSlotRulesPerDay: TimeSlotRulesJson? = null,
            val daysBeforePeriod: Int? = null,
            val planningStartHour: Int? = null,
            val reminderIntervalDays: Int? = null,
        )

        data class TimeSlotRulesJson(
            val mondays: LocalTime? = null,
            val tuesdays: LocalTime? = null,
            val wednesdays: LocalTime? = null,
            val thursdays: LocalTime? = null,
            val fridays: LocalTime? = null,
            val saturdays: LocalTime? = null,
            val sundays: LocalTime? = null,
        ) {
            companion object {
                val EVERY_DAY_DEFAULT = TimeSlotRulesJson(
                    mondays = LocalTime.of(20, 30),
                    tuesdays = LocalTime.of(20, 30),
                    wednesdays = LocalTime.of(20, 30),
                    thursdays = LocalTime.of(20, 30),
                    fridays = LocalTime.of(20, 30),
                    saturdays = LocalTime.of(20, 30),
                    sundays = LocalTime.of(20, 30),
                )
            }
        }

        enum class SchedulingType { WEEKLY, MONTHLY }
    }

    companion object {
        private const val CONFIGURATION_FILE_NAME = "configuration.json"
    }
}
