package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.cronjobs.AvailabilityMessageSender
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ConfigurationMainScreen(context: OperationContext) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        listOf(
            TextDisplay.of("# Time Sage configuration"),
            TextDisplay.of(
                "${DiscordFormatter.mentionChannel(context.channelId)} in " +
                        JDAHolder.jda.getGuildById(context.guildId)?.name
            ),
            TextDisplay.of("-# All configuration is specific for this channel in this server."),
            Section.of(
                if (configuration.enabled) {
                    Buttons.Disable(this).render()
                } else {
                    Buttons.Enable(this).render()
                },
                TextDisplay.of(DiscordFormatter.bold("Enabled") + ": " + (if (configuration.enabled) "Yes" else "No"))
            ),
            TextDisplay.of(DiscordFormatter.bold("Activities") + ":"),
        ) + if (configuration.activities.isEmpty()) {
            listOf(TextDisplay.of(DiscordFormatter.italics("There are currently no activities.")))
        } else {
            configuration.activities.map {
                Section.of(
                    Buttons.EditActivity(it.id, this).render(),
                    TextDisplay.of("- ${it.name}")
                )
            }
        } + listOf(
            ActionRow.of(
                Buttons.AddActivity(this).render()
            )
        )

    override fun parameters(): List<String> = emptyList()

    companion object {
        fun reconstruct(parameters: List<String>, context: OperationContext) = ConfigurationMainScreen(context)
    }

    class Buttons {
        class Enable(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Enable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.context) { it.enabled = true }
                    AvailabilityMessageSender.postAvailabilityMessage(screen.context)
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Enable>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Enable::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    Enable(screen = reconstruct(parameters = screenParameters, context = context))
            }
        }

        class Disable(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Disable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.context) { it.enabled = false }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Disable>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Disable::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    Disable(screen = reconstruct(parameters = screenParameters, context = context))
            }
        }

        class EditActivity(private val activityId: Int, screen: ConfigurationMainScreen) :
            ScreenButton<ConfigurationMainScreen>(
                screen = screen
            ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationActivityScreen(activityId, screen.context) }
            }

            override fun parameters(): List<String> = listOf(activityId.toString())

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, EditActivity>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = EditActivity::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    EditActivity(
                        activityId = componentParameters.first().toInt(),
                        screen = reconstruct(parameters = screenParameters, context = context)
                    )
            }
        }

        class AddActivity(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Add new activity")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { interactionHook ->
                    val newActivityId = ConfigurationRepository.update(screen.context) { configuration ->
                        ConfigurationRepository.MutableConfiguration.MutableActivity(
                            id = (configuration.activities.maxOfOrNull { it.id } ?: 0) + 1,
                            name = "New Activity",
                            participants = mutableListOf(
                                ConfigurationRepository.MutableConfiguration.MutableActivity.MutableParticipant(
                                    userId = interactionHook.interaction.user.idLong,
                                    optional = false
                                )
                            ),
                            maxMissingOptionalParticipants = 0
                        ).also(configuration.activities::add).id
                    }
                    ConfigurationActivityScreen(newActivityId, screen.context)
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, AddActivity>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = AddActivity::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    AddActivity(screen = reconstruct(parameters = screenParameters, context = context))
            }
        }
    }
}
