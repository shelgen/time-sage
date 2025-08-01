package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.LocalDate

class AvailabilityReminderScreen(val startDate: LocalDate, context: OperationContext) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val week = AvailabilitiesWeekRepository.loadOrInitialize(startDate = startDate, context = context)

        val unansweredParticipants = configuration.activities
            .flatMap(Activity::participants)
            .map(Participant::userId)
            .filter { week.responses.forUserId(it) == null }
            .distinct()
            .sorted()

        return if (unansweredParticipants.isNotEmpty()) {
            val channel = JDAHolder.jda.getTextChannelById(context.channelId)!!
            listOf(
                TextDisplay.of(
                    "Hey ${unansweredParticipants.joinToString(separator = ", ") { DiscordFormatter.mentionUser(it) }}!" +
                            " Looks like I'm missing your availability for the next week's schedule." +
                            " Could you check it out? ${"https://discord.com/channels/${channel.guild.idLong}/${channel.idLong}/${week.messageId}"}"
                )
            )
        } else {
            emptyList()
        }
    }

    override fun parameters(): List<String> = listOf(startDate.toString())

    companion object {
        fun reconstruct(parameters: List<String>, context: OperationContext) =
            AvailabilityReminderScreen(startDate = LocalDate.parse(parameters.first()), context)
    }
}
