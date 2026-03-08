package com.github.shelgen.timesage

import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.plan.Plan
import net.dv8tion.jda.api.entities.Guild
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val SESSION_DURATION_HOURS = 3L

fun createScheduledEventsForPlan(plan: Plan, guild: Guild, configuration: Configuration) {
    plan.sessions.forEach { session ->
        val activity = configuration.getActivity(session.activityId)
        val voiceChannel = activity.voiceChannel?.let { guild.voiceChannelCache.getElementById(it.id) }
        val startTime: OffsetDateTime = session.timeSlot.atOffset(ZoneOffset.UTC)

        val (ifNeedBe, attending) = session.participants.partition { it.ifNeedBe }
        val description = buildString {
            if (attending.isNotEmpty()) appendLine("Attending: ${attending.joinToString { it.user.toMention() }}")
            if (ifNeedBe.isNotEmpty()) append("If need be: ${ifNeedBe.joinToString { it.user.toMention() }}")
        }.trimEnd()

        val action = if (voiceChannel != null) {
            guild.createScheduledEvent(activity.name, voiceChannel, startTime)
        } else {
            guild.createScheduledEvent(activity.name, "Online", startTime, startTime.plusHours(SESSION_DURATION_HOURS))
        }
        if (description.isNotEmpty()) action.setDescription(description)
        action.queue()
    }
}
