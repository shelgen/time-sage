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
        timeSlotRules = timeSlotRules?.map { it.toDomain() } ?: listOf(TimeSlotRule.DEFAULT)
    )

    private fun ConfigurationFileDao.Json.SchedulingType.toDomain() = when (this) {
        ConfigurationFileDao.Json.SchedulingType.WEEKLY -> SchedulingType.WEEKLY
        ConfigurationFileDao.Json.SchedulingType.MONTHLY -> SchedulingType.MONTHLY
    }

    private fun ConfigurationFileDao.Json.SlotRule.toDomain() = TimeSlotRule(
        dayType = dayType.toDomain(),
        timeOfDay = timeOfDayUtc ?: timeOfDay!!,
    )

    private fun ConfigurationFileDao.Json.DayType.toDomain(): DayType = when (this) {
        ConfigurationFileDao.Json.DayType.MONDAYS -> DayType.MONDAYS
        ConfigurationFileDao.Json.DayType.TUESDAYS -> DayType.TUESDAYS
        ConfigurationFileDao.Json.DayType.WEDNESDAYS -> DayType.WEDNESDAYS
        ConfigurationFileDao.Json.DayType.THURSDAYS -> DayType.THURSDAYS
        ConfigurationFileDao.Json.DayType.FRIDAYS -> DayType.FRIDAYS
        ConfigurationFileDao.Json.DayType.SATURDAYS -> DayType.SATURDAYS
        ConfigurationFileDao.Json.DayType.SUNDAYS -> DayType.SUNDAYS
        ConfigurationFileDao.Json.DayType.WEEKDAYS -> DayType.WEEKDAYS
        ConfigurationFileDao.Json.DayType.WEEKENDS -> DayType.WEEKENDS
        ConfigurationFileDao.Json.DayType.EVERY_DAY -> DayType.EVERY_DAY
    }

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
        timeSlotRules = timeSlotRules.map { it.toJson() }
    )

    private fun SchedulingType.toJson() = when (this) {
        SchedulingType.WEEKLY -> ConfigurationFileDao.Json.SchedulingType.WEEKLY
        SchedulingType.MONTHLY -> ConfigurationFileDao.Json.SchedulingType.MONTHLY
    }

    private fun TimeSlotRule.toJson() = ConfigurationFileDao.Json.SlotRule(
        dayType = dayType.toJson(),
        timeOfDayUtc = null,
        timeOfDay = timeOfDay,
    )

    private fun DayType.toJson(): ConfigurationFileDao.Json.DayType = when (this) {
        DayType.MONDAYS -> ConfigurationFileDao.Json.DayType.MONDAYS
        DayType.TUESDAYS -> ConfigurationFileDao.Json.DayType.TUESDAYS
        DayType.WEDNESDAYS -> ConfigurationFileDao.Json.DayType.WEDNESDAYS
        DayType.THURSDAYS -> ConfigurationFileDao.Json.DayType.THURSDAYS
        DayType.FRIDAYS -> ConfigurationFileDao.Json.DayType.FRIDAYS
        DayType.SATURDAYS -> ConfigurationFileDao.Json.DayType.SATURDAYS
        DayType.SUNDAYS -> ConfigurationFileDao.Json.DayType.SUNDAYS
        DayType.WEEKDAYS -> ConfigurationFileDao.Json.DayType.WEEKDAYS
        DayType.WEEKENDS -> ConfigurationFileDao.Json.DayType.WEEKENDS
        DayType.EVERY_DAY -> ConfigurationFileDao.Json.DayType.EVERY_DAY
    }

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
