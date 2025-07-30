package com.github.shelgen.timesage

import com.github.shelgen.timesage.cronjobs.CronJobScheduling
import com.github.shelgen.timesage.slashcommands.AbstractSlashCommand
import com.github.shelgen.timesage.ui.screens.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
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
        MDC.putCloseable(MDC_GUILD_ID, event.guild?.idLong?.toString()).use {
            MDC.putCloseable(MDC_USER_NAME, event.user.name).use {
                val id = event.componentId
                logger.info("Received ButtonInteractionEvent (custom id $id)")
                val button = CustomIdSerialization.deserialize(
                    customId = id,
                    expectedType = ScreenButton::class,
                    guildId = event.guild!!.idLong
                )
                button.handle(event)
            }
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        MDC.putCloseable(MDC_GUILD_ID, event.guild?.idLong?.toString()).use {
            MDC.putCloseable(MDC_USER_NAME, event.user.name).use {
                val id = event.componentId
                logger.info("Received EntitySelectInteractionEvent (custom id $id)")
                val selectMenu = CustomIdSerialization.deserialize(
                    customId = id,
                    expectedType = ScreenEntitySelectMenu::class,
                    guildId = event.guild!!.idLong
                )
                selectMenu.handle(event)
            }
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        MDC.putCloseable(MDC_GUILD_ID, event.guild?.idLong?.toString()).use {
            MDC.putCloseable(MDC_USER_NAME, event.user.name).use {
                val id = event.componentId
                logger.info("Received StringSelectInteractionEvent (custom id $id)")
                val selectMenu = CustomIdSerialization.deserialize(
                    customId = id,
                    expectedType = ScreenStringSelectMenu::class,
                    guildId = event.guild!!.idLong
                )
                selectMenu.handle(event)
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        MDC.putCloseable(MDC_GUILD_ID, event.guild?.idLong?.toString()).use {
            MDC.putCloseable(MDC_USER_NAME, event.user.name).use {
                val id = event.modalId
                logger.info("Received ModalInteractionEvent (custom id $id)")
                val modal = CustomIdSerialization.deserialize(
                    customId = id,
                    expectedType = ScreenModal::class,
                    guildId = event.guild!!.idLong
                )
                modal.handle(event)
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        MDC.putCloseable(MDC_GUILD_ID, event.guild?.idLong?.toString()).use {
            MDC.putCloseable(MDC_USER_NAME, event.user.name).use {
                val name = event.name
                logger.info("Received SlashCommandInteractionEvent (name $name)")
                val command = slashCommands.find { command -> command.name == name }
                if (command == null) {
                    event.reply("Sorry, I don't recognize the command you just used. Maybe it's an outdated one?")
                        .queue()
                } else {
                    command.handle(
                        event = event,
                        guildId = event.guild!!.idLong
                    )
                }
            }
        }
    }
}
