package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.cronjobs.AvailabilityMessageSender
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DayType
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.TimeSlotRule
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.modals.Modal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

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
            Section.of(
                Buttons.ChangeTimeZone(this).render(),
                TextDisplay.of("${DiscordFormatter.bold("Time zone")}: ${configuration.timeZone.toZoneId()}")
            ),
            TextDisplay.of("${DiscordFormatter.bold("Schedule interval")}: ${configuration.scheduling.type.humanReadableName}\n"),
            Section.of(
                Buttons.EditScheduling(this).render(),
                TextDisplay.of("### Scheduling")
            ),
            TextDisplay.of(
                "Weeks start on a ${
                    DiscordFormatter.bold(
                        configuration.scheduling.startDayOfWeek.getDisplayName(
                            TextStyle.FULL,
                            Locale.US
                        ).lowercase()
                    )
                }.\n" +
                        "Eligible times are " +
                        configuration.scheduling.timeSlotRules.first().let { timeSlotRule ->
                            DiscordFormatter.bold(timeSlotRule.dayType.humanReadableName) +
                                    " at ${DiscordFormatter.bold(timeSlotRule.timeOfDay.toString())}"
                        }
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

        class ChangeTimeZone(screen: ConfigurationMainScreen) :
            ScreenButton<ConfigurationMainScreen>(
                screen = screen
            ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change time zone...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationTimeZoneMainScreen(screen.context) }
            }

            override fun parameters(): List<String> = listOf()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, ChangeTimeZone>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = ChangeTimeZone::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    ChangeTimeZone(
                        screen = reconstruct(parameters = screenParameters, context = context)
                    )
            }
        }

        class EditScheduling(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit scheduling...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.EditScheduling(screen).render(configuration)
                event.replyModal(modal).queue()
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, EditScheduling>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = EditScheduling::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    EditScheduling(
                        screen = reconstruct(
                            parameters = screenParameters,
                            context = context
                        )
                    )
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
                    val newActivityId = ConfigurationRepository.update(screen.context) { it.addNewActivity() }
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

    class Modals {
        class EditScheduling(screen: ConfigurationMainScreen) : ScreenModal<ConfigurationMainScreen>(screen) {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit scheduling")
                    .addComponents(
                        Label.of(
                            "Start day of week",
                            StringSelectMenu.create("startDayOfWeek")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(DayOfWeek.entries.map {
                                    SelectOption.of(
                                        it.getDisplayName(TextStyle.FULL, Locale.US),
                                        it.value.toString()
                                    )
                                })
                                .setDefaultValues(configuration.scheduling.startDayOfWeek.value.toString())
                                .build()
                        ),
                        Label.of(
                            "Eligible days",
                            StringSelectMenu.create("dayType")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(DayType.entries.map {
                                    SelectOption.of(
                                        it.humanReadableName,
                                        it.name
                                    )
                                })
                                .setDefaultValues(configuration.scheduling.timeSlotRules.first().dayType.name)
                                .build()
                        ),
                        Label.of(
                            "Start time on eligible days",
                            TextInput.create("timeOfDay", TextInputStyle.SHORT)
                                .setValue(configuration.scheduling.timeSlotRules.first().timeOfDay.toString())
                                .build()
                        ),
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        configuration.scheduling.startDayOfWeek =
                            DayOfWeek.of(event.getValue("startDayOfWeek")!!.asStringList.first().toInt())
                        val oldTimeOfDay = configuration.scheduling.timeSlotRules.first().timeOfDay
                        configuration.scheduling.timeSlotRules.clear()
                        configuration.scheduling.timeSlotRules.add(
                            TimeSlotRule(
                                dayType = enumValueOf(event.getValue("dayType")!!.asStringList.first()),
                                timeOfDay = runCatching {
                                    LocalTime.parse(event.getValue("timeOfDay")!!.asString)
                                }.getOrNull() ?: oldTimeOfDay
                            )
                        )
                        DayOfWeek.of(event.getValue("startDayOfWeek")!!.asStringList.first().toInt())
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, EditScheduling>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = EditScheduling::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    EditScheduling(
                        screen = reconstruct(
                            parameters = screenParameters,
                            context = context
                        )
                    )
            }
        }
    }
}
