package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.LocalDate

class AvailabilityReminderScreen(val weekMondayDate: LocalDate, guildId: Long) : Screen(guildId) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val week = WeekRepository.loadOrInitialize(guildId = guildId, mondayDate = weekMondayDate)

        val unansweredParticipants = configuration.activities
            .flatMap(Activity::participants)
            .map(Participant::userId)
            .filter { week.responses[it] == null }
            .distinct()
            .sorted()

        return if (unansweredParticipants.isNotEmpty()) {
            val channelId = configuration.channelId
            if (channelId != null) {
                val channel = JDAHolder.jda.getTextChannelById(channelId)!!
                listOf(
                    TextDisplay.of(
                        "Hey ${unansweredParticipants.joinToString(separator = ", ") { DiscordFormatter.mentionUser(it) }}!" +
                                " Looks like I'm missing your availability for the next week's schedule." +
                                " Could you check it out? ${"https://discord.com/channels/${channel.guild.idLong}/${channel.idLong}/${week.weekAvailabilityMessageDiscordId}"}"
                    )
                )
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override fun parameters(): List<String> = listOf(weekMondayDate.toString())

    companion object {
        fun reconstruct(parameters: List<String>, guildId: Long) =
            AvailabilityReminderScreen(weekMondayDate = LocalDate.parse(parameters.first()), guildId)
    }
}
