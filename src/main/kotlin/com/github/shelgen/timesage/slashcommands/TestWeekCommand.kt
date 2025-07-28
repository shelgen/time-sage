package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.cronjobs.SendNextWeeksAvailabilityMessageJob
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object TestWeekCommand : AbstractSlashCommand(
    name = "tstestweek",
    description = "Launch a week availability message",
) {
    override fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply().queue {
            SendNextWeeksAvailabilityMessageJob().sendMessage()
            it.deleteOriginal().queue()
        }
    }
}
