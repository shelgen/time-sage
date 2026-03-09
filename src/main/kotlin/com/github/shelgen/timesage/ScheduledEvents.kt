package com.github.shelgen.timesage

import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.discord.DiscordScheduledEventId
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.Session
import net.dv8tion.jda.api.entities.Guild
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val SESSION_DURATION_HOURS = 3L

fun createScheduledEventsForPlan(
    plan: Plan,
    guild: Guild,
    configuration: Configuration,
    onComplete: (List<DiscordScheduledEventId>) -> Unit,
) {
    createEventsRecursive(plan.sessions, guild, configuration, emptyList(), onComplete)
}

private fun createEventsRecursive(
    remaining: List<Session>,
    guild: Guild,
    configuration: Configuration,
    accumulated: List<DiscordScheduledEventId>,
    onComplete: (List<DiscordScheduledEventId>) -> Unit,
) {
    if (remaining.isEmpty()) {
        onComplete(accumulated)
        return
    }
    val session = remaining.first()
    val rest = remaining.drop(1)
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
    action.queue { event ->
        createEventsRecursive(
            remaining = rest,
            guild = guild,
            configuration = configuration,
            accumulated = accumulated + DiscordScheduledEventId(event.idLong),
            onComplete = onComplete,
        )
    }
}
