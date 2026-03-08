package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.ui.screens.PlanningProcessesScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

@Suppress("unused")
object PlanningCommand : AbstractSlashCommand(
    name = "tsplanning",
    description = "Manage planning processes",
) {
    override fun handle(event: SlashCommandInteractionEvent, tenant: Tenant) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            it.editOriginal(PlanningProcessesScreen(tenant).renderEdit()).queue()
            MDC.clear()
        }
    }
}
