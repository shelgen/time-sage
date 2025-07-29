package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.ui.screens.ConfigurationMainScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object ConfigureCommand : AbstractSlashCommand(
    name = "tsconfigure",
    description = "Configures Time Sage",
) {
    override fun handle(event: SlashCommandInteractionEvent, guildId: Long) {
        event.deferReply(true).queue {
            it.sendMessage(ConfigurationMainScreen(guildId).render()).queue()
        }
    }
}
