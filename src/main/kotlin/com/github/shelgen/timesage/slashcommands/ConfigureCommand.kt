package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.ui.screens.ConfigurationMainScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

object ConfigureCommand : AbstractSlashCommand(
    name = "tsconfigure",
    description = "Configures Time Sage",
) {
    override fun handle(event: SlashCommandInteractionEvent, guildId: Long) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            it.sendMessage(ConfigurationMainScreen(guildId).render()).queue()
            MDC.clear()
        }
    }
}
