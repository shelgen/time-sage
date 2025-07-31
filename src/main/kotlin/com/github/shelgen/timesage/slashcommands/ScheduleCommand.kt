package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.ui.screens.PlanAlternativeListScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

object ScheduleCommand : AbstractSlashCommand(
    name = "tsschedule",
    description = "Attempt to schedule next week's sessions",
) {
    override fun handle(event: SlashCommandInteractionEvent, context: OperationContext) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            it.sendMessage(
                PlanAlternativeListScreen(
                    weekMondayDate = nextMonday(),
                    startIndex = 0,
                    size = 3,
                    context = context
                ).render()
            ).queue()
            MDC.clear()
        }
    }
}
