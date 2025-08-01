package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import java.time.DayOfWeek

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(context: OperationContext): Configuration =
        dao.load(context)
            ?.toConfiguration()
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

    private fun ConfigurationFileDao.Json.toConfiguration(): Configuration =
        Configuration(
            enabled = enabled,
            scheduling = scheduling?.toScheduling() ?: Configuration.Scheduling.DEFAULT,
            activities = activities.map { it.toActivity() },
        )

    private fun ConfigurationFileDao.Json.Scheduling.toScheduling() = Configuration.Scheduling(
        type = when (type) {
            ConfigurationFileDao.Json.SchedulingType.WEEKLY -> Configuration.SchedulingType.WEEKLY
        },
        startDayOfWeek = startDayOfWeek
    )

    private fun ConfigurationFileDao.Json.Activity.toActivity(): Activity =
        Activity(
            id = id,
            name = name,
            participants = participants.map { it.toParticipant() },
            maxMissingOptionalParticipants = this.maxMissingOptionalParticipants
        )

    private fun ConfigurationFileDao.Json.Participant.toParticipant(): Participant =
        Participant(userId = userId, optional = optional)

    data class MutableConfiguration(
        var enabled: Boolean,
        val scheduling: MutableScheduling,
        val activities: MutableSet<MutableActivity>
    ) {
        constructor(configuration: Configuration) : this(
            enabled = configuration.enabled,
            scheduling = MutableScheduling(configuration.scheduling),
            activities = configuration.activities.map(::MutableActivity).toMutableSet()
        )

        fun getActivity(activityId: Int): MutableActivity =
            activities.first { it.id == activityId }

        data class MutableScheduling(
            val type: Configuration.SchedulingType,
            val startDayOfWeek: DayOfWeek
        ) {
            constructor(scheduling: Configuration.Scheduling) : this(
                type = scheduling.type,
                startDayOfWeek = scheduling.startDayOfWeek
            )
        }

        data class MutableActivity(
            val id: Int,
            var name: String,
            val participants: MutableList<MutableParticipant>,
            var maxMissingOptionalParticipants: Int
        ) {
            constructor(activity: Activity) : this(
                id = activity.id,
                name = activity.name,
                participants = activity.participants.map(::MutableParticipant).toMutableList(),
                maxMissingOptionalParticipants = activity.maxMissingOptionalParticipants
            )

            data class MutableParticipant(val userId: Long, var optional: Boolean) {
                constructor(participant: Participant) : this(
                    userId = participant.userId,
                    optional = participant.optional
                )

                fun toJson(): ConfigurationFileDao.Json.Participant =
                    ConfigurationFileDao.Json.Participant(
                        userId = userId,
                        optional = optional
                    )
            }

            fun toJson(): ConfigurationFileDao.Json.Activity =
                ConfigurationFileDao.Json.Activity(
                    id = id,
                    name = name,
                    participants = participants.sortedBy { it.userId }.map { it.toJson() },
                    maxMissingOptionalParticipants = maxMissingOptionalParticipants
                )

            fun setRequiredParticipants(userIds: List<Long>) {
                userIds.distinct().forEach { userId ->
                    val participant = participants.firstOrNull { it.userId == userId }
                    if (participant == null) {
                        participants.add(MutableParticipant(userId = userId, optional = false))
                    } else {
                        participant.optional = false
                    }
                }
            }

            fun setOptionalParticipants(userIds: List<Long>) {
                userIds.distinct().forEach { userId ->
                    val participant = participants.firstOrNull { it.userId == userId }
                    if (participant == null) {
                        participants.add(MutableParticipant(userId = userId, optional = true))
                    } else {
                        participant.optional = true
                    }
                }
            }
        }

        fun toJson(): ConfigurationFileDao.Json =
            ConfigurationFileDao.Json(
                enabled = enabled,
                scheduling = ConfigurationFileDao.Json.Scheduling(
                    type = when (scheduling.type) {
                        Configuration.SchedulingType.WEEKLY -> ConfigurationFileDao.Json.SchedulingType.WEEKLY
                    },
                    startDayOfWeek = scheduling.startDayOfWeek
                ),
                activities = activities.sortedBy { it.id }.map { it.toJson() },
            )
    }
}
