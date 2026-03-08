package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.Tenant
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

@Suppress("unused")
object ScheduleCommand : AbstractSlashCommand(
    name = "tsschedule",
    description = "Attempt to schedule next period's sessions",
) {
    override fun handle(event: SlashCommandInteractionEvent, tenant: Tenant) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            /* TODO: Something exciting is coming here!
            val configuration = ConfigurationRepository.loadOrInitialize(tenant)
            val dateRange = configuration.activePeriod()
            val screen = PlanAlternativesPageScreen(
                dateRange = dateRange,
                fromInclusive = 0,
                pageSize = 3,
                tenant = tenant
            )
            it.sendMessage(screen.render()).queue()
             */
            MDC.clear()
        }
    }
}
