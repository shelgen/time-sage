package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.ui.screens.PlanAlternativeListScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object ScheduleCommand : AbstractSlashCommand(
    name = "tsschedule",
    description = "Attempt to schedule next week's sessions",
) {
    override fun handle(event: SlashCommandInteractionEvent) {
        event.deferReply(true).queue {
            it.sendMessage(
                PlanAlternativeListScreen(
                    weekMondayDate = nextMonday(),
                    startIndex = 0,
                    size = 3
                ).render()
            ).queue()
        }
    }
}
