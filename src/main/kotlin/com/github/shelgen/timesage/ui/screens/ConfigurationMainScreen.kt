package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.domain.TimeSlotRules
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.modals.Modal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

class ConfigurationMainScreen(tenant: Tenant) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val orderedDays = (0..6).map {
            DayOfWeek.of((configuration.localization.startDayOfWeek.value - 1 + it) % 7 + 1)
        }
        return listOf(
            TextDisplay.of("# Time Sage configuration"),
            TextDisplay.of(
                "${DiscordFormatter.mentionChannel(tenant.textChannel)} in " +
                        JDAHolder.jda.getGuildById(tenant.server)?.name
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
                Buttons.SetVoiceChannel(this).render(),
                TextDisplay.of(
                    "${DiscordFormatter.bold("Voice channel for scheduled events")}: " +
                            (configuration.voiceChannelId?.let { id ->
                                JDAHolder.jda.getGuildById(tenant.server)
                                    ?.voiceChannelCache?.getElementById(id)?.name
                                    ?.let { "#$it" } ?: "Unknown channel"
                            } ?: "Not set")
                )
            ),
            TextDisplay.of("### Localization"),
            Section.of(
                Buttons.ChangeTimeZone(this).render(),
                TextDisplay.of("${DiscordFormatter.bold("Time zone")}: ${configuration.localization.timeZone.toZoneId()}")
            ),
            Section.of(
                Buttons.EditStartDay(this).render(),
                TextDisplay.of(
                    "Weeks start on a ${
                        DiscordFormatter.bold(
                            configuration.localization.startDayOfWeek.getDisplayName(TextStyle.FULL, Locale.US).lowercase()
                        )
                    }."
                )
            ),
            Section.of(
                Buttons.ChangeScheduleInterval(this).render(),
                TextDisplay.of("${DiscordFormatter.bold("Schedule interval")}: ${configuration.scheduling.type.humanReadableName}")
            ),
            TextDisplay.of("### Scheduling"),
            ActionRow.of(
                Buttons.EditWeekdays(this).render(),
                Buttons.EditWeekend(this).render()
            ),
            TextDisplay.of(
                "Eligible times are " +
                        orderedDays.mapNotNull { day ->
                            configuration.scheduling.timeSlotRules[day]?.let { time ->
                                DiscordFormatter.bold(day.getDisplayName(TextStyle.FULL, Locale.US) + "s") +
                                        " at ${DiscordFormatter.bold(time.toString())}"
                            }
                        }.joinToString(", ")
            ),
            Section.of(
                Buttons.EditPlanningTiming(this).render(),
                TextDisplay.of("### Planning timing")
            ),
            TextDisplay.of(
                "Planning starts ${DiscordFormatter.bold("${configuration.scheduling.numDaysInAdvanceToStartPlanning} days")} before the period at " +
                        DiscordFormatter.bold("%02d:00".format(configuration.scheduling.timeOfDayToStartPlanning)) + " (${configuration.localization.timeZone.toZoneId()}).\n" +
                        "Reminders: " + when (configuration.scheduling.reminderIntervalDays) {
                    0 -> DiscordFormatter.bold("Never")
                    1 -> DiscordFormatter.bold("Every day")
                    7 -> DiscordFormatter.bold("Every week")
                    else -> DiscordFormatter.bold("Every ${configuration.scheduling.reminderIntervalDays} days")
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
    }

    class Buttons {
        class Enable(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Enable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.tenant) { it.enabled = true }
                }
            }
        }

        class Disable(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Disable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.tenant) { it.enabled = false }
                }
            }
        }

        class SetVoiceChannel(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Set voice channel...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.SetVoiceChannel(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class ChangeTimeZone(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change time zone...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationTimeZoneMainScreen(screen.tenant) }
            }
        }

        class ChangeScheduleInterval(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change schedule interval...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.ChangeScheduleInterval(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class EditWeekdays(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit weekdays...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.EditWeekdays(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class EditWeekend(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit weekend...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.EditWeekend(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class EditStartDay(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change start day...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.EditStartDay(screen).render(configuration)
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
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.EditActivity(activityId, screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class AddActivity(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Add new activity...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.AddActivity(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class DeleteActivities(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "Delete activities...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    ConfigurationDeleteActivitiesScreen(screen.tenant)
                }
            }
        }

        class EditPlanningTiming(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit planning timing...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                val modal = Modals.EditPlanningTiming(screen).render(configuration)
                event.replyModal(modal).queue()
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
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
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
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        configuration.voiceChannelId = event.getValue("voiceChannel")!!.asLongList.firstOrNull()
                    }
                }
            }
        }

        class EditWeekdays(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration): Modal {
                val weekdays = listOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                )
                return Modal
                    .create(CustomIdSerialization.serialize(this), "Edit weekday scheduling")
                    .addComponents(
                        *weekdays.map { day ->
                            Label.of(
                                "${day.getDisplayName(TextStyle.FULL, Locale.US)} (leave blank to skip)",
                                TextInput.create(day.name.lowercase(), TextInputStyle.SHORT)
                                    .setRequired(false)
                                    .also { builder ->
                                        configuration.scheduling.timeSlotRules[day]
                                            ?.let { builder.setValue(it.toString()) }
                                    }
                                    .build()
                            )
                        }.toTypedArray()
                    )
                    .build()
            }

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        val weekdays = listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        )
                        val updatedPairs = weekdays.mapNotNull { day ->
                            val raw = event.getValue(day.name.lowercase())?.asString?.trim()
                            if (!raw.isNullOrBlank()) runCatching { LocalTime.parse(raw) }.getOrNull()?.let { day to it }
                            else null
                        }
                        val preserved = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).mapNotNull { day ->
                            configuration.scheduling.timeSlotRules[day]?.let { day to it }
                        }
                        configuration.scheduling.timeSlotRules =
                            TimeSlotRules.of(*(updatedPairs + preserved).toTypedArray())
                    }
                }
            }
        }

        class EditWeekend(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration): Modal {
                val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                return Modal
                    .create(CustomIdSerialization.serialize(this), "Edit weekend scheduling")
                    .addComponents(
                        *weekend.map { day ->
                            Label.of(
                                "${day.getDisplayName(TextStyle.FULL, Locale.US)} (leave blank to skip)",
                                TextInput.create(day.name.lowercase(), TextInputStyle.SHORT)
                                    .setRequired(false)
                                    .also { builder ->
                                        configuration.scheduling.timeSlotRules[day]
                                            ?.let { builder.setValue(it.toString()) }
                                    }
                                    .build()
                            )
                        }.toTypedArray()
                    )
                    .build()
            }

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        val updatedPairs = weekend.mapNotNull { day ->
                            val raw = event.getValue(day.name.lowercase())?.asString?.trim()
                            if (!raw.isNullOrBlank()) runCatching { LocalTime.parse(raw) }.getOrNull()?.let { day to it }
                            else null
                        }
                        val preserved = listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        ).mapNotNull { day ->
                            configuration.scheduling.timeSlotRules[day]?.let { day to it }
                        }
                        configuration.scheduling.timeSlotRules =
                            TimeSlotRules.of(*(updatedPairs + preserved).toTypedArray())
                    }
                }
            }
        }

        class EditStartDay(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Change start day of week")
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
                                .setDefaultValues(configuration.localization.startDayOfWeek.value.toString())
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        configuration.localization.startDayOfWeek =
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
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        val activity = configuration.addNewActivity()
                        activity.name = event.getValue("name")!!.asString
                        activity.setOptionalMembers(event.getValue("optionalParticipants")!!.asLongList)
                        activity.setRequiredMembers(event.getValue("requiredParticipants")!!.asLongList)
                        activity.maxNumMissingOptionalMembers =
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
                                        .members
                                        .filterNot(ActivityMember::optional)
                                        .map(ActivityMember::userId)
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
                                        .members
                                        .filter(ActivityMember::optional)
                                        .map(ActivityMember::userId)
                                        .map(EntitySelectMenu.DefaultValue::user)
                                ).build()
                        ),
                        Label.of(
                            "Max missing optional participants",
                            StringSelectMenu.create("maxMissingOptional")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..4).map(Int::toString).map { SelectOption.of(it, it) })
                                .setDefaultValues(configuration.getActivity(activityId).maxNumMissingOptionalMembers.toString())
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        val activity = configuration.getActivity(activityId = activityId)
                        activity.name = event.getValue("name")!!.asString
                        activity.setOptionalMembers(event.getValue("optionalParticipants")!!.asLongList)
                        activity.setRequiredMembers(event.getValue("requiredParticipants")!!.asLongList)
                        activity.maxNumMissingOptionalMembers =
                            event.getValue("maxMissingOptional")!!.asStringList.first().toInt()
                    }
                }
            }
        }

        class EditPlanningTiming(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit planning timing")
                    .addComponents(
                        Label.of(
                            "Days before period",
                            StringSelectMenu.create("daysBeforePeriod")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((1..14).map {
                                    SelectOption.of(if (it == 1) "1 day before" else "$it days before", it.toString())
                                })
                                .setDefaultValues(configuration.scheduling.numDaysInAdvanceToStartPlanning.coerceIn(1, 14).toString())
                                .build()
                        ),
                        Label.of(
                            "Planning start hour (${configuration.localization.timeZone.toZoneId()})",
                            StringSelectMenu.create("planningStartHour")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..23).map { SelectOption.of("%02d:00".format(it), it.toString()) })
                                .setDefaultValues(configuration.scheduling.timeOfDayToStartPlanning.toString())
                                .build()
                        ),
                        Label.of(
                            "Reminder interval",
                            StringSelectMenu.create("reminderIntervalDays")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(listOf(
                                    SelectOption.of("Never", "0"),
                                    SelectOption.of("Every day", "1"),
                                    SelectOption.of("Every other day", "2"),
                                    SelectOption.of("Every 3 days", "3"),
                                    SelectOption.of("Every 4 days", "4"),
                                    SelectOption.of("Every 5 days", "5"),
                                    SelectOption.of("Every 6 days", "6"),
                                    SelectOption.of("Every week", "7"),
                                ))
                                .setDefaultValues(configuration.scheduling.reminderIntervalDays.coerceIn(0, 7).toString())
                                .build()
                        ),
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        configuration.scheduling.numDaysInAdvanceToStartPlanning =
                            event.getValue("daysBeforePeriod")!!.asStringList.first().toInt()
                        configuration.scheduling.timeOfDayToStartPlanning =
                            event.getValue("planningStartHour")!!.asStringList.first().toInt()
                        configuration.scheduling.reminderIntervalDays =
                            event.getValue("reminderIntervalDays")!!.asStringList.first().toInt()
                    }
                }
            }
        }
    }
}
