package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.Participant

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(guildId: Long): Configuration = dao.loadOrInitialize(guildId).toConfiguration(guildId)

    fun <T> update(
        guildId: Long,
        modification: (configuration: MutableConfiguration) -> T
    ): T {
        val configuration = loadOrInitialize(guildId)
        val mutableConfiguration = MutableConfiguration(configuration)
        val returnValue = modification(mutableConfiguration)
        dao.save(guildId, mutableConfiguration.toJson())
        return returnValue
    }

    fun findAllGuildIds() = dao.findAllGuildIds()

    private fun ConfigurationFileDao.Json.toConfiguration(guildId: Long): Configuration =
        Configuration(
            guildId = guildId,
            enabled = enabled,
            channelId = channelId,
            activities = activities.map { it.toActivity() },
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
        val guildId: Long,
        var enabled: Boolean,
        var channelId: Long?,
        val activities: MutableSet<MutableActivity>
    ) {
        constructor(configuration: Configuration) : this(
            guildId = configuration.guildId,
            enabled = configuration.enabled,
            channelId = configuration.channelId,
            activities = configuration.activities.map(::MutableActivity).toMutableSet()
        )

        fun getActivity(activityId: Int): MutableActivity =
            activities.first { it.id == activityId }

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
                channelId = channelId,
                activities = activities.sortedBy { it.id }.map { it.toJson() },
            )
    }
}
