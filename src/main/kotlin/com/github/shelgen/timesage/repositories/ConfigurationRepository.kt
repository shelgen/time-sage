package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.*

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(context: OperationContext): Configuration =
        dao.load(context)
            ?.toDomain()
            ?: Configuration.DEFAULT

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
        scheduling = scheduling?.toDomain() ?: Scheduling.DEFAULT,
        activities = activities.map { it.toDomain() },
    )

    private fun ConfigurationFileDao.Json.Scheduling.toDomain() = Scheduling(
        type = type.toDomain(),
        startDayOfWeek = startDayOfWeek
    )

    private fun ConfigurationFileDao.Json.SchedulingType.toDomain() = when (this) {
        ConfigurationFileDao.Json.SchedulingType.WEEKLY -> SchedulingType.WEEKLY
    }

    private fun ConfigurationFileDao.Json.Activity.toDomain() = Activity(
        id = id,
        name = name,
        participants = participants.map { it.toDomain() },
        maxMissingOptionalParticipants = this.maxMissingOptionalParticipants
    )

    private fun ConfigurationFileDao.Json.Participant.toDomain() = Participant(
        userId = userId,
        optional = optional
    )

    private fun MutableConfiguration.toJson() = ConfigurationFileDao.Json(
        enabled = enabled,
        scheduling = scheduling.toJson(),
        activities = activities.sortedBy { it.id }.map { it.toJson() },
    )

    private fun MutableScheduling.toJson() = ConfigurationFileDao.Json.Scheduling(
        type = type.toJson(),
        startDayOfWeek = startDayOfWeek
    )

    private fun SchedulingType.toJson() = when (this) {
        SchedulingType.WEEKLY -> ConfigurationFileDao.Json.SchedulingType.WEEKLY
    }

    private fun MutableActivity.toJson() = ConfigurationFileDao.Json.Activity(
        id = id,
        name = name,
        participants = participants.sortedBy { it.userId }.map { it.toJson() },
        maxMissingOptionalParticipants = maxMissingOptionalParticipants
    )

    private fun MutableParticipant.toJson() = ConfigurationFileDao.Json.Participant(
        userId = userId,
        optional = optional
    )
}
