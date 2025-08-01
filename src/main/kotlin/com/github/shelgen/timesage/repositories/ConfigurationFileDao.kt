package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
import java.io.File
import java.time.DayOfWeek

class ConfigurationFileDao {
    private val fileDao = CachedJsonFileDao<Json>(
        jsonClass = Json::class.java,
        initialContent = Json(
            enabled = false,
            scheduling = Json.Scheduling(
                type = Json.SchedulingType.WEEKLY,
                startDayOfWeek = DayOfWeek.MONDAY,
            ),
            activities = emptyList()
        )
    )

    fun save(context: OperationContext, json: Json) {
        fileDao.save(getConfigurationFile(context), json)
    }

    fun loadOrInitialize(context: OperationContext): Json =
        fileDao.loadOrInitialize(getConfigurationFile(context))

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
            val participants: List<Participant>,
            val maxMissingOptionalParticipants: Int
        )

        data class Scheduling(
            val type: SchedulingType,
            val startDayOfWeek: DayOfWeek
        )

        enum class SchedulingType { WEEKLY }

        data class Participant(val userId: Long, val optional: Boolean)
    }

    companion object {
        private const val CONFIGURATION_FILE_NAME = "configuration.json"
    }
}
