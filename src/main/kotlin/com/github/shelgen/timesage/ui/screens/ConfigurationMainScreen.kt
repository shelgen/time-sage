package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.cronjobs.AvailabilityMessageSender
import com.github.shelgen.timesage.domain.*
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.entities.channel.ChannelType
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
            Section.of(
                Buttons.SetVoiceChannel(this).render(),
                TextDisplay.of(
                    "${DiscordFormatter.bold("Voice channel for scheduled events")}: " +
                            (configuration.voiceChannelId?.let { id ->
                                JDAHolder.jda.getGuildById(context.guildId)
                                    ?.voiceChannelCache?.getElementById(id)?.name
                                    ?.let { "#$it" } ?: "Unknown channel"
                            } ?: "Not set")
                )
            ),
            Section.of(
                Buttons.ChangeScheduleInterval(this).render(),
                TextDisplay.of("${DiscordFormatter.bold("Schedule interval")}: ${configuration.scheduling.type.humanReadableName}")
            ),
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
            listOfNotNull(
                Buttons.AddActivity(this).render(),
                if (configuration.activities.isEmpty()) null else Buttons.DeleteActivities(this).render()
            ).let { ActionRow.of(it) }
        )

    class Buttons {
        class Enable(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Enable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.context) { it.enabled = true }
                    AvailabilityMessageSender.postAvailabilityMessage(screen.context)
                }
            }
        }

        class Disable(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Disable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.context) { it.enabled = false }
                }
            }
        }

        class SetVoiceChannel(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Set voice channel...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.SetVoiceChannel(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class ChangeTimeZone(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change time zone...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationTimeZoneMainScreen(screen.context) }
            }
        }

        class ChangeScheduleInterval(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change schedule interval...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.ChangeScheduleInterval(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class EditScheduling(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit scheduling...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.EditScheduling(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class EditActivity(
            private val activityId: Int,
            override val screen: ConfigurationMainScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.EditActivity(activityId, screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class AddActivity(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Add new activity...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.AddActivity(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class DeleteActivities(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "Delete activities...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    ConfigurationDeleteActivitiesScreen(screen.context)
                }
            }
        }
    }

    class Modals {
        class ChangeScheduleInterval(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Change schedule interval")
                    .addComponents(
                        Label.of(
                            "Schedule interval",
                            StringSelectMenu.create("scheduleInterval")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(SchedulingType.entries.map {
                                    SelectOption.of(it.humanReadableName, it.name)
                                })
                                .setDefaultValues(configuration.scheduling.type.name)
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        configuration.scheduling.type =
                            enumValueOf(event.getValue("scheduleInterval")!!.asStringList.first())
                    }
                }
            }
        }

        class SetVoiceChannel(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Set voice channel for scheduled events")
                    .addComponents(
                        Label.of(
                            "Voice channel",
                            EntitySelectMenu.create("voiceChannel", SelectTarget.CHANNEL)
                                .setChannelTypes(ChannelType.VOICE)
                                .setRequired(false)
                                .setMinValues(0)
                                .setMaxValues(1)
                                .also { builder ->
                                    configuration.voiceChannelId?.let {
                                        builder.setDefaultValues(EntitySelectMenu.DefaultValue.channel(it))
                                    }
                                }
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        configuration.voiceChannelId = event.getValue("voiceChannel")!!.asLongList.firstOrNull()
                    }
                }
            }
        }

        class EditScheduling(override val screen: ConfigurationMainScreen) : ScreenModal {
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
        }

        class AddActivity(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Add activity")
                    .addComponents(
                        Label.of(
                            "Name",
                            TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder("Name of the activity")
                                .build()
                        ),
                        Label.of(
                            "Required participants",
                            EntitySelectMenu.create("requiredParticipants", SelectTarget.USER)
                                .setMinValues(1)
                                .setMaxValues(25)
                                .build()
                        ),
                        Label.of(
                            "Optional participants",
                            EntitySelectMenu.create("optionalParticipants", SelectTarget.USER)
                                .setRequired(false)
                                .setMinValues(0)
                                .setMaxValues(25)
                                .build()
                        ),
                        Label.of(
                            "Max missing optional participants",
                            StringSelectMenu.create("maxMissingOptional")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..4).map(Int::toString).map { SelectOption.of(it, it) })
                                .setDefaultValues("1")
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        val activity = configuration.addNewActivity()
                        activity.name = event.getValue("name")!!.asString
                        activity.setOptionalParticipants(event.getValue("optionalParticipants")!!.asLongList)
                        activity.setRequiredParticipants(event.getValue("requiredParticipants")!!.asLongList)
                        activity.maxMissingOptionalParticipants =
                            event.getValue("maxMissingOptional")!!.asStringList.first().toInt()
                    }
                }
            }
        }

        class EditActivity(private val activityId: Int, override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit activity")
                    .addComponents(
                        Label.of(
                            "Name",
                            TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder("Name of the activity")
                                .setValue(configuration.getActivity(activityId).name)
                                .build()
                        ),
                        Label.of(
                            "Required participants",
                            EntitySelectMenu.create("requiredParticipants", SelectTarget.USER)
                                .setMinValues(1)
                                .setMaxValues(25)
                                .setDefaultValues(
                                    configuration.getActivity(activityId)
                                        .participants
                                        .filterNot(Participant::optional)
                                        .map(Participant::userId)
                                        .map(EntitySelectMenu.DefaultValue::user)
                                ).build()
                        ),
                        Label.of(
                            "Optional participants",
                            EntitySelectMenu.create("optionalParticipants", SelectTarget.USER)
                                .setRequired(false)
                                .setMinValues(0)
                                .setMaxValues(25)
                                .setDefaultValues(
                                    configuration.getActivity(activityId)
                                        .participants
                                        .filter(Participant::optional)
                                        .map(Participant::userId)
                                        .map(EntitySelectMenu.DefaultValue::user)
                                ).build()
                        ),
                        Label.of(
                            "Max missing optional participants",
                            StringSelectMenu.create("maxMissingOptional")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..4).map(Int::toString).map { SelectOption.of(it, it) })
                                .setDefaultValues(configuration.getActivity(activityId).maxMissingOptionalParticipants.toString())
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        val activity = configuration.getActivity(activityId = activityId)
                        activity.name = event.getValue("name")!!.asString
                        activity.setOptionalParticipants(event.getValue("optionalParticipants")!!.asLongList)
                        activity.setRequiredParticipants(event.getValue("requiredParticipants")!!.asLongList)
                        activity.maxMissingOptionalParticipants =
                            event.getValue("maxMissingOptional")!!.asStringList.first().toInt()
                    }
                }
            }
        }
    }
}
