package com.github.shelgen.timesage

import com.github.shelgen.timesage.cronjobs.CronJobScheduling
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.slashcommands.AbstractSlashCommand
import com.github.shelgen.timesage.ui.screens.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class TimeSage : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val slashCommands: List<AbstractSlashCommand> =
        AbstractSlashCommand::class.sealedSubclasses.map { it.objectInstance!! }

    fun start() {
        setUpCommands()
        JDAHolder.jda.addEventListener(this)
        CronJobScheduling.setUp()
    }

    private fun setUpCommands() {
        JDAHolder.jda.updateCommands()
            .addCommands(
                slashCommands.map { command ->
                    Commands.slash(command.name, command.description)
                        .apply { defaultPermissions = DefaultMemberPermissions.DISABLED }
                }
            )
            .queue {
                logger.info("Updated command list for bot")
            }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        withContextAndUserMDC(event.toOperationContext(), event.user.name) { context ->
            val id = event.componentId
            logger.info("Received ButtonInteractionEvent (custom id $id)")
            try {
                val button = CustomIdSerialization.deserialize<ScreenButton>(
                    customId = id,
                    context = context
                )
                button.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle ButtonInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        withContextAndUserMDC(event.toOperationContext(), event.user.name) { context ->
            val id = event.componentId
            logger.info("Received EntitySelectInteractionEvent (custom id $id)")
            try {
                val selectMenu = CustomIdSerialization.deserialize<ScreenEntitySelectMenu>(
                    customId = id,
                    context = context
                )
                selectMenu.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle EntitySelectInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        withContextAndUserMDC(event.toOperationContext(), event.user.name) { context ->
            val id = event.componentId
            logger.info("Received StringSelectInteractionEvent (custom id $id)")
            try {
                val selectMenu = CustomIdSerialization.deserialize<ScreenStringSelectMenu>(
                    customId = id,
                    context = context
                )
                selectMenu.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle StringSelectInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        withContextAndUserMDC(event.toOperationContext(), event.user.name) { context ->
            val id = event.modalId
            logger.info("Received ModalInteractionEvent (custom id $id)")
            try {
                val modal = CustomIdSerialization.deserialize<ScreenModal>(
                    customId = id,
                    context = context
                )
                modal.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle ModalInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        withContextAndUserMDC(event.toOperationContext(), event.user.name) { context ->
            val name = event.name
            logger.info("Received SlashCommandInteractionEvent (name $name)")
            try {
                val command = slashCommands.find { command -> command.name == name }
                if (command == null) {
                    event.reply("Sorry, I don't recognize the command you just used. Maybe it's an outdated one?")
                        .queue()
                } else {
                    command.handle(
                        event = event,
                        context = context
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to handle SlashCommandInteractionEvent (name $name)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    private fun withUser(name: String, block: (OperationContext) -> Unit) {
        MDC.putCloseable(MDC_USER_NAME, name).use({ block })
    }

    private fun Interaction.toOperationContext(): OperationContext =
        OperationContext(guildId = guild!!.idLong, channelId = channel!!.idLong)
}
