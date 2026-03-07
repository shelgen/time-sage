package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*
import java.time.DayOfWeek
import java.util.*

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(tenant: Tenant): Configuration =
        dao.load(tenant)
            ?.toDomain(tenant)
            ?: Configuration.createDefault(tenant)

    @Synchronized
    fun <T> update(
        tenant: Tenant,
        modification: (configuration: MutableConfiguration) -> T
    ): T {
        val configuration = loadOrInitialize(tenant)
        val mutableConfiguration = MutableConfiguration(configuration)
        val returnValue = modification(mutableConfiguration)
        dao.save(tenant, mutableConfiguration.toJson())
        return returnValue
    }

    fun findAllTenants() = getAllTenants()

    private fun ConfigurationFileDao.Json.toDomain(tenant: Tenant): Configuration {
        return Configuration(
            enabled = enabled,
            localization = Localization(
                timeZone = timeZone ?: TimeZone.getTimeZone("UTC"),
                startDayOfWeek = scheduling.startDayOfWeek,
            ),
            scheduling = scheduling.toDomain(),
            activities = activities.map { it.toDomain() },
            voiceChannelId = voiceChannelId
        )
    }

    private fun ConfigurationFileDao.Json.Scheduling.toDomain() = Scheduling(
        type = type.toDomain(),
        timeSlotRules = timeSlotRulesPerDay.toDomain(),
        numDaysInAdvanceToStartPlanning = daysBeforePeriod,
        timeOfDayToStartPlanning = planningStartHour,
        reminderIntervalDays = reminderIntervalDays,
    )

    private fun ConfigurationFileDao.Json.Scheduling.Type.toDomain() = when (this) {
        ConfigurationFileDao.Json.Scheduling.Type.WEEKLY -> SchedulingType.WEEKLY
        ConfigurationFileDao.Json.Scheduling.Type.MONTHLY -> SchedulingType.MONTHLY
    }

    private fun ConfigurationFileDao.Json.TimeSlotRules.toDomain() = TimeSlotRules(
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
            members = participants.toDomain(),
            maxMissingOptionalMembers = this.maxMissingOptionalParticipants
        )
    }

    private fun ConfigurationFileDao.Json.Participants.toDomain(): List<ActivityMember> =
        required.map { ActivityMember(userId = it, optional = false) } +
                optional.map { ActivityMember(userId = it, optional = true) }

    private fun Configuration.toJson() = ConfigurationFileDao.Json(
        enabled = enabled,
        timeZone = localization.timeZone,
        scheduling = scheduling.toJson(localization.startDayOfWeek),
        activities = activities.sortedBy { it.id }.map { it.toJson() },
        voiceChannelId = voiceChannelId
    )

    private fun Scheduling.toJson(startDayOfWeek: DayOfWeek) = ConfigurationFileDao.Json.Scheduling(
        type = type.toJson(),
        startDayOfWeek = startDayOfWeek,
        timeSlotRulesPerDay = timeSlotRules.toJson(),
        daysBeforePeriod = numDaysInAdvanceToStartPlanning,
        planningStartHour = timeOfDayToStartPlanning,
        reminderIntervalDays = reminderIntervalDays,
    )

    private fun SchedulingType.toJson() = when (this) {
        SchedulingType.WEEKLY -> ConfigurationFileDao.Json.Scheduling.Type.WEEKLY
        SchedulingType.MONTHLY -> ConfigurationFileDao.Json.Scheduling.Type.MONTHLY
    }

    private fun TimeSlotRules.toJson() = ConfigurationFileDao.Json.TimeSlotRules(
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
        participants = members.toJson(),
        maxMissingOptionalParticipants = maxMissingOptionalMembers
    )

    private fun List<ActivityMember>.toJson() = ConfigurationFileDao.Json.Participants(
        required = filterNot { it.optional }.map { it.userId }.toSortedSet(),
        optional = filter { it.optional }.map { it.userId }.toSortedSet(),
    )
}
