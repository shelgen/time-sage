package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.ui.screens.ConfigurationMainScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

@Suppress("unused")
object ConfigureCommand : AbstractSlashCommand(
    name = "tsconfigure",
    description = "Configures Time Sage",
) {
    override fun handle(event: SlashCommandInteractionEvent, tenant: Tenant) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            it.sendMessage(ConfigurationMainScreen(tenant).render()).queue()
            MDC.clear()
        }
    }
}
