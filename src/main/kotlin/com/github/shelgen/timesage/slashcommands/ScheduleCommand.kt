package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.PlanAlternativesPageScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

object ScheduleCommand : AbstractSlashCommand(
    name = "tsschedule",
    description = "Attempt to schedule next period's sessions",
) {
    override fun handle(event: SlashCommandInteractionEvent, tenant: Tenant) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            val configuration = ConfigurationRepository.loadOrInitialize(tenant)
            val targetPeriod = configuration.activePeriod()
            val screen = PlanAlternativesPageScreen(
                targetPeriod = targetPeriod,
                fromInclusive = 0,
                pageSize = 3,
                tenant = tenant
            )
            it.sendMessage(screen.render()).queue()
            MDC.clear()
        }
    }
}
