package com.github.shelgen.timesage

import com.github.shelgen.timesage.cronjobs.CronJobScheduling
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.slashcommands.AbstractSlashCommand
import com.github.shelgen.timesage.ui.screens.CustomIdSerialization
import com.github.shelgen.timesage.ui.screens.ScreenButton
import com.github.shelgen.timesage.ui.screens.ScreenEntitySelectMenu
import com.github.shelgen.timesage.ui.screens.ScreenModal
import com.github.shelgen.timesage.ui.screens.ScreenStringSelectMenu
import com.github.shelgen.timesage.ui.screens.allSealedSubclasses
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
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
        AbstractSlashCommand::class.allSealedSubclasses().map { it.objectInstance!! }

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
        withTenantAndUserMDC(event.toTenant(), event.user.name) { tenant ->
            val id = event.componentId
            logger.info("Received ButtonInteractionEvent (custom id $id)")
            try {
                val button = CustomIdSerialization.deserialize<ScreenButton>(
                    customId = id,
                    tenant = tenant
                )
                button.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle ButtonInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        withTenantAndUserMDC(event.toTenant(), event.user.name) { tenant ->
            val id = event.componentId
            logger.info("Received EntitySelectInteractionEvent (custom id $id)")
            try {
                val selectMenu = CustomIdSerialization.deserialize<ScreenEntitySelectMenu>(
                    customId = id,
                    tenant = tenant
                )
                selectMenu.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle EntitySelectInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        withTenantAndUserMDC(event.toTenant(), event.user.name) { tenant ->
            val id = event.componentId
            logger.info("Received StringSelectInteractionEvent (custom id $id)")
            try {
                val selectMenu = CustomIdSerialization.deserialize<ScreenStringSelectMenu>(
                    customId = id,
                    tenant = tenant
                )
                selectMenu.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle StringSelectInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        withTenantAndUserMDC(event.toTenant(), event.user.name) { tenant ->
            val id = event.modalId
            logger.info("Received ModalInteractionEvent (custom id $id)")
            try {
                val modal = CustomIdSerialization.deserialize<ScreenModal>(
                    customId = id,
                    tenant = tenant
                )
                modal.handle(event)
            } catch (e: Exception) {
                logger.error("Failed to handle ModalInteractionEvent (custom id $id)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        withTenantAndUserMDC(event.toTenant(), event.user.name) { tenant ->
            val name = event.name
            logger.info("Received SlashCommandInteractionEvent (name $name)")
            try {
                val command = slashCommands.find { command -> command.name == name }
                if (command == null) {
                    event.reply("Sorry, I don't recognize the command you just used. Maybe it's an outdated one?")
                        .setEphemeral(true).queue()
                } else {
                    command.handle(
                        event = event,
                        tenant = tenant
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to handle SlashCommandInteractionEvent (name $name)", e)
                runCatching { event.reply("Something went wrong. Please try again.").setEphemeral(true).queue() }
            }
        }
    }

    private fun withUser(name: String, block: (Tenant) -> Unit) {
        MDC.putCloseable(MDC_USER_NAME, name).use({ block })
    }

    private fun Interaction.toTenant(): Tenant {
        val channel = channel!!
        val textChannelId = if (channel is ThreadChannel) channel.parentChannel.idLong else channel.idLong
        return Tenant(server = DiscordServerId(guild!!.idLong), textChannel = DiscordTextChannelId(textChannelId))
    }
}
