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
        scheduling = scheduling.toJson(),
        activities = activities.sortedBy { it.id }.map { it.toJson() },
    )

    private fun Scheduling.toJson() = ConfigurationFileDao.Json.Scheduling(
        type = type.toJson(),
        startDayOfWeek = startDayOfWeek
    )

    private fun SchedulingType.toJson() = when (this) {
        SchedulingType.WEEKLY -> ConfigurationFileDao.Json.SchedulingType.WEEKLY
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
