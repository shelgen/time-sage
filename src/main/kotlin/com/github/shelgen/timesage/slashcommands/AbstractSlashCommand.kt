package com.github.shelgen.timesage.slashcommands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

sealed class AbstractSlashCommand(
    val name: String,
    val description: String
) {
    abstract fun handle(event: SlashCommandInteractionEvent)
}
