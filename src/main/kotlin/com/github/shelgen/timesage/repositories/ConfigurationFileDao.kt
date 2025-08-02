package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime

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
        val scheduling: Scheduling,
        val activities: List<Activity>
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
            val timeSlotRules: List<SlotRule>?
        )

        enum class SchedulingType { WEEKLY }

        data class SlotRule(
            val dayType: DayType,
            val timeOfDayUtc: LocalTime
        )

        enum class DayType {
            MONDAYS, TUESDAYS, WEDNESDAYS, THURSDAYS, FRIDAYS, SATURDAYS, SUNDAYS, WEEKDAYS, WEEKENDS, EVERY_DAY
        }
    }

    companion object {
        private const val CONFIGURATION_FILE_NAME = "configuration.json"
    }
}
