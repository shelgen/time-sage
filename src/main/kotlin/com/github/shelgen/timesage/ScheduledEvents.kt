package com.github.shelgen.timesage

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.planning.Plan
import net.dv8tion.jda.api.entities.Guild
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val SESSION_DURATION_HOURS = 3L

fun createScheduledEventsForPlan(guild: Guild, plan: Plan, configuration: Configuration) {
    val voiceChannel = configuration.voiceChannelId?.let { guild.voiceChannelCache.getElementById(it) }

    plan.sessions.forEach { session ->
        val activity = configuration.getActivity(session.activityId)
        val startTime: OffsetDateTime = session.timeSlot.atOffset(ZoneOffset.UTC)

        val attending = session.attendees.filter { !it.ifNeedBe }.map { "<@${it.userId}>" }
        val ifNeedBe = session.attendees.filter { it.ifNeedBe }.map { "<@${it.userId}>" }
        val description = buildString {
            if (attending.isNotEmpty()) appendLine("Attending: ${attending.joinToString()}")
            if (ifNeedBe.isNotEmpty()) append("If need be: ${ifNeedBe.joinToString()}")
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
