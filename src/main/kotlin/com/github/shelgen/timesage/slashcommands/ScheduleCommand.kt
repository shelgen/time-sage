package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.PlanAlternativeListScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

object ScheduleCommand : AbstractSlashCommand(
    name = "tsschedule",
    description = "Attempt to schedule next period's sessions",
) {
    override fun handle(event: SlashCommandInteractionEvent, context: OperationContext) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            val configuration = ConfigurationRepository.loadOrInitialize(context)
            val period = configuration.scheduling.activePeriod(configuration.timeZone)
            val screen = PlanAlternativeListScreen(
                periodStart = period.fromDate,
                periodEnd = period.toDate,
                startIndex = 0,
                size = 3,
                context = context
            )
            it.sendMessage(screen.render()).queue()
            MDC.clear()
        }
    }
}
