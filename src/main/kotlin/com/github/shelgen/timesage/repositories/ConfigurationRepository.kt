package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*
import java.util.*

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(context: OperationContext): Configuration =
        dao.load(context)
            ?.toDomain()
            ?: Configuration.DEFAULT

    @Synchronized
    fun <T> update(
        context: OperationContext,
        modification: (configuration: MutableConfiguration) -> T
    ): T {
        val configuration = loadOrInitialize(context)
        val mutableConfiguration = MutableConfiguration(configuration)
        val returnValue = modification(mutableConfiguration)
        dao.save(context, mutableConfiguration.toJson())
        return returnValue
    }

    fun findAllOperationContexts() = dao.findAllOperationContexts()

    private fun ConfigurationFileDao.Json.toDomain() = Configuration(
        enabled = enabled,
        timeZone = timeZone ?: TimeZone.getTimeZone("UTC"),
        scheduling = scheduling.toDomain(),
        activities = activities.map { it.toDomain() },
        voiceChannelId = voiceChannelId
    )

    private fun ConfigurationFileDao.Json.Scheduling.toDomain() = Scheduling(
        type = type.toDomain(),
        startDayOfWeek = startDayOfWeek,
        timeSlotRules = (timeSlotRulesPerDay ?: ConfigurationFileDao.Json.TimeSlotRulesJson.EVERY_DAY_DEFAULT).toDomain(),
        daysBeforePeriod = daysBeforePeriod ?: 5,
        planningStartHour = planningStartHour ?: 17,
        reminderIntervalDays = reminderIntervalDays ?: 1,
    )

    private fun ConfigurationFileDao.Json.SchedulingType.toDomain() = when (this) {
        ConfigurationFileDao.Json.SchedulingType.WEEKLY -> SchedulingType.WEEKLY
        ConfigurationFileDao.Json.SchedulingType.MONTHLY -> SchedulingType.MONTHLY
    }

    private fun ConfigurationFileDao.Json.TimeSlotRulesJson.toDomain() = TimeSlotRules(
        mondays = mondays,
        tuesdays = tuesdays,
        wednesdays = wednesdays,
        thursdays = thursdays,
        fridays = fridays,
        saturdays = saturdays,
        sundays = sundays,
    )

    private fun ConfigurationFileDao.Json.Activity.toDomain(): Activity {
        return Activity(
            id = id,
            name = name,
            participants = participants.toDomain(),
            maxMissingOptionalParticipants = this.maxMissingOptionalParticipants
        )
    }

    private fun ConfigurationFileDao.Json.Participants.toDomain(): List<Participant> =
        required.map { Participant(userId = it, optional = false) } +
                optional.map { Participant(userId = it, optional = true) }

    private fun Configuration.toJson() = ConfigurationFileDao.Json(
        enabled = enabled,
        timeZone = timeZone,
        scheduling = scheduling.toJson(),
        activities = activities.sortedBy { it.id }.map { it.toJson() },
        voiceChannelId = voiceChannelId
    )

    private fun Scheduling.toJson() = ConfigurationFileDao.Json.Scheduling(
        type = type.toJson(),
        startDayOfWeek = startDayOfWeek,
        timeSlotRulesPerDay = timeSlotRules.toJson(),
        daysBeforePeriod = daysBeforePeriod,
        planningStartHour = planningStartHour,
        reminderIntervalDays = reminderIntervalDays,
    )

    private fun SchedulingType.toJson() = when (this) {
        SchedulingType.WEEKLY -> ConfigurationFileDao.Json.SchedulingType.WEEKLY
        SchedulingType.MONTHLY -> ConfigurationFileDao.Json.SchedulingType.MONTHLY
    }

    private fun TimeSlotRules.toJson() = ConfigurationFileDao.Json.TimeSlotRulesJson(
        mondays = mondays,
        tuesdays = tuesdays,
        wednesdays = wednesdays,
        thursdays = thursdays,
        fridays = fridays,
        saturdays = saturdays,
        sundays = sundays,
    )

    private fun Activity.toJson() = ConfigurationFileDao.Json.Activity(
        id = id,
        name = name,
        participants = participants.toJson(),
        maxMissingOptionalParticipants = maxMissingOptionalParticipants
    )

    private fun List<Participant>.toJson() = ConfigurationFileDao.Json.Participants(
        required = filterNot { it.optional }.map { it.userId }.toSortedSet(),
        optional = filter { it.optional }.map { it.userId }.toSortedSet(),
    )
}
