package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Interval
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.discord.DiscordVoiceChannelId
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
            TextDisplay.of(
                "# Time Sage configuration\n" +
                        "${tenant.textChannel.toMention()} in " + JDAHolder.getGuild(tenant).name + "\n" +
                        "-# All configuration is specific for this channel in this server.\n" +
                        "### Localization"
            ),
            Section.of(
                Buttons.ChangeTimeZone(this).render(),
                TextDisplay.of("${DiscordFormatter.bold("Time zone")}: ${configuration.localization.timeZone.toZoneId()}")
            ),
            Section.of(
                Buttons.EditStartDay(this).render(),
                TextDisplay.of(
                    "Weeks start on a " + DiscordFormatter.bold(
                        configuration.localization.startDayOfWeek
                            .getDisplayName(TextStyle.FULL, Locale.US)
                            .lowercase()
                    )
                )
            ),
            TextDisplay.of("### Time slot rules"),
            ActionRow.of(
                Buttons.EditWeekdays(this).render(),
                Buttons.EditWeekend(this).render()
            ),
            TextDisplay.of(
                orderedDays.mapNotNull { day ->
                    configuration.timeSlotRules[day]?.let { time ->
                        DiscordFormatter.bold(day.getDisplayName(TextStyle.FULL, Locale.US) + "s") +
                                " at ${DiscordFormatter.bold(time.toString())}"
                    }
                }.joinToString(", ").ifEmpty { DiscordFormatter.italics("No time slots configured.") }
            ),
            Section.of(
                Buttons.EditPeriodicPlanning(this).render(),
                TextDisplay.of("### Periodic planning")
            ),
            TextDisplay.of(
                if (!configuration.periodicPlanning.enabled) {
                    DiscordFormatter.italics("Periodic planning is disabled.")
                } else {
                    "Automatic planning is done ${
                        DiscordFormatter.bold(configuration.periodicPlanning.interval.name.lowercase())
                    }, starting ${DiscordFormatter.bold("${configuration.periodicPlanning.daysInAdvance} days")} before each period " +
                            "at ${DiscordFormatter.bold("%02d:00".format(configuration.periodicPlanning.hourOfDay))} (${configuration.localization.timeZone.toZoneId()})."
                }
            ),
            Section.of(
                Buttons.EditReminders(this).render(),
                TextDisplay.of("### Reminders")
            ),
            TextDisplay.of(
                if (!configuration.reminders.enabled) {
                    DiscordFormatter.italics("Reminders are disabled.")
                } else {
                    "Reminders are sent every " + when (configuration.reminders.intervalDays) {
                        1 -> DiscordFormatter.bold("day")
                        7 -> DiscordFormatter.bold("week")
                        else -> DiscordFormatter.bold("${configuration.reminders.intervalDays} days")
                    } + " at ${DiscordFormatter.bold("%02d:00".format(configuration.reminders.hourOfDay))} (${configuration.localization.timeZone.toZoneId()})."
                }
            ),
            TextDisplay.of(DiscordFormatter.bold("Activities") + ":"),
        ) + if (configuration.activities.isEmpty()) {
            listOf(TextDisplay.of(DiscordFormatter.italics("There are currently no activities.")))
        } else {
            configuration.activities.map { activity ->
                Section.of(
                    Buttons.EditActivity(activity.id, this).render(),
                    TextDisplay.of("- ${activity.name}")
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
        class ChangeTimeZone(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change time zone...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationTimeZoneMainScreen(screen.tenant) }
            }
        }

        class EditStartDay(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Change start day...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.EditStartDay(screen).render(configuration)).queue()
            }
        }

        class EditWeekdays(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit weekdays...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.EditWeekdays(screen).render(configuration)).queue()
            }
        }

        class EditWeekend(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit weekend...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.EditWeekend(screen).render(configuration)).queue()
            }
        }

        class EditPeriodicPlanning(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit periodic planning...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.EditPeriodicPlanning(screen).render(configuration)).queue()
            }
        }

        class EditReminders(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit reminders...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.EditReminders(screen).render(configuration)).queue()
            }
        }

        class EditActivity(
            private val activityId: ActivityId,
            override val screen: ConfigurationMainScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.EditActivity(activityId, screen).render(configuration)).queue()
            }
        }

        class AddActivity(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Add new activity...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                event.replyModal(Modals.AddActivity(screen).render(configuration)).queue()
            }
        }

        class DeleteActivities(override val screen: ConfigurationMainScreen) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "Delete activities...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationDeleteActivitiesScreen(screen.tenant) }
            }
        }
    }

    class Modals {
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
                                "${day.getDisplayName(TextStyle.FULL, Locale.US)} (leave blank to remove)",
                                TextInput.create(day.name.lowercase(), TextInputStyle.SHORT)
                                    .setRequired(false)
                                    .also { builder ->
                                        configuration.timeSlotRules[day]
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
                        listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        ).forEach { day ->
                            val raw = event.getValue(day.name.lowercase())?.asString?.trim()
                            if (!raw.isNullOrBlank()) {
                                runCatching { LocalTime.parse(raw) }.getOrNull()
                                    ?.let { configuration.timeSlotRules[day] = it }
                            } else {
                                configuration.timeSlotRules.remove(day)
                            }
                        }
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
                                "${day.getDisplayName(TextStyle.FULL, Locale.US)} (leave blank to remove)",
                                TextInput.create(day.name.lowercase(), TextInputStyle.SHORT)
                                    .setRequired(false)
                                    .also { builder ->
                                        configuration.timeSlotRules[day]
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
                        listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { day ->
                            val raw = event.getValue(day.name.lowercase())?.asString?.trim()
                            if (!raw.isNullOrBlank()) {
                                runCatching { LocalTime.parse(raw) }.getOrNull()
                                    ?.let { configuration.timeSlotRules[day] = it }
                            } else {
                                configuration.timeSlotRules.remove(day)
                            }
                        }
                    }
                }
            }
        }

        class EditPeriodicPlanning(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit periodic planning")
                    .addComponents(
                        Label.of(
                            "Enabled",
                            StringSelectMenu.create("enabled")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(
                                    SelectOption.of("Yes", "true"),
                                    SelectOption.of("No", "false")
                                )
                                .setDefaultValues(configuration.periodicPlanning.enabled.toString())
                                .build()
                        ),
                        Label.of(
                            "Period type",
                            StringSelectMenu.create("periodType")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(Interval.entries.map {
                                    SelectOption.of(
                                        it.name.lowercase().replaceFirstChar(Char::uppercaseChar),
                                        it.name
                                    )
                                })
                                .setDefaultValues(configuration.periodicPlanning.interval.name)
                                .build()
                        ),
                        Label.of(
                            "Days before period",
                            StringSelectMenu.create("daysBeforePeriod")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((1..14).map {
                                    SelectOption.of(if (it == 1) "1 day before" else "$it days before", it.toString())
                                })
                                .setDefaultValues(
                                    configuration.periodicPlanning.daysInAdvance.coerceIn(1, 14).toString()
                                )
                                .build()
                        ),
                        Label.of(
                            "Planning start hour (${configuration.localization.timeZone.toZoneId()})",
                            StringSelectMenu.create("planningStartHour")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..23).map { SelectOption.of("%02d:00".format(it), it.toString()) })
                                .setDefaultValues(configuration.periodicPlanning.hourOfDay.toString())
                                .build()
                        ),
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        configuration.periodicPlanning.enabled =
                            event.getValue("enabled")!!.asStringList.first().toBoolean()
                        configuration.periodicPlanning.interval =
                            enumValueOf(event.getValue("periodType")!!.asStringList.first())
                        configuration.periodicPlanning.daysInAdvance =
                            event.getValue("daysBeforePeriod")!!.asStringList.first().toInt()
                        configuration.periodicPlanning.hourOfDay =
                            event.getValue("planningStartHour")!!.asStringList.first().toInt()
                    }
                }
            }
        }

        class EditReminders(override val screen: ConfigurationMainScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit reminders")
                    .addComponents(
                        Label.of(
                            "Enabled",
                            StringSelectMenu.create("remindersEnabled")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(
                                    SelectOption.of("Yes", "true"),
                                    SelectOption.of("No", "false")
                                )
                                .setDefaultValues(configuration.reminders.enabled.toString())
                                .build()
                        ),
                        Label.of(
                            "Reminder interval",
                            StringSelectMenu.create("reminderIntervalDays")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions(
                                    listOf(
                                        SelectOption.of("Every day", "1"),
                                        SelectOption.of("Every other day", "2"),
                                        SelectOption.of("Every 3 days", "3"),
                                        SelectOption.of("Every 4 days", "4"),
                                        SelectOption.of("Every 5 days", "5"),
                                        SelectOption.of("Every 6 days", "6"),
                                        SelectOption.of("Every week", "7"),
                                    )
                                )
                                .setDefaultValues(configuration.reminders.intervalDays.coerceIn(1, 7).toString())
                                .build()
                        ),
                        Label.of(
                            "Reminder hour (${configuration.localization.timeZone.toZoneId()})",
                            StringSelectMenu.create("reminderHour")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..23).map { SelectOption.of("%02d:00".format(it), it.toString()) })
                                .setDefaultValues(configuration.reminders.hourOfDay.toString())
                                .build()
                        ),
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        configuration.reminders.enabled =
                            event.getValue("remindersEnabled")!!.asStringList.first().toBoolean()
                        configuration.reminders.intervalDays =
                            event.getValue("reminderIntervalDays")!!.asStringList.first().toInt()
                        configuration.reminders.hourOfDay =
                            event.getValue("reminderHour")!!.asStringList.first().toInt()
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
                        ),
                        Label.of(
                            "Voice channel (optional)",
                            EntitySelectMenu.create("voiceChannel", SelectTarget.CHANNEL)
                                .setChannelTypes(ChannelType.VOICE)
                                .setRequired(false)
                                .setMinValues(0)
                                .setMaxValues(1)
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        val activity = configuration.addNewActivity()
                        activity.name = event.getValue("name")!!.asString
                        activity.setRequiredMembers(event.getValue("requiredParticipants")!!.asLongList.map(::DiscordUserId))
                        activity.setOptionalMembers(event.getValue("optionalParticipants")!!.asLongList.map(::DiscordUserId))
                        activity.maxNumMissingOptionalMembers =
                            event.getValue("maxMissingOptional")!!.asStringList.first().toInt()
                        activity.voiceChannel =
                            event.getValue("voiceChannel")!!.asLongList.firstOrNull()?.let(::DiscordVoiceChannelId)
                    }
                }
            }
        }

        class EditActivity(
            private val activityId: ActivityId,
            override val screen: ConfigurationMainScreen
        ) : ScreenModal {
            fun render(configuration: Configuration): Modal {
                val activity = configuration.getActivity(activityId)
                return Modal
                    .create(CustomIdSerialization.serialize(this), "Edit activity")
                    .addComponents(
                        Label.of(
                            "Name",
                            TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder("Name of the activity")
                                .setValue(activity.name)
                                .build()
                        ),
                        Label.of(
                            "Required participants",
                            EntitySelectMenu.create("requiredParticipants", SelectTarget.USER)
                                .setMinValues(1)
                                .setMaxValues(25)
                                .setDefaultValues(
                                    activity.members
                                        .filterNot(Member::optional)
                                        .map { EntitySelectMenu.DefaultValue.user(it.user.id) }
                                ).build()
                        ),
                        Label.of(
                            "Optional participants",
                            EntitySelectMenu.create("optionalParticipants", SelectTarget.USER)
                                .setRequired(false)
                                .setMinValues(0)
                                .setMaxValues(25)
                                .setDefaultValues(
                                    activity.members
                                        .filter(Member::optional)
                                        .map { EntitySelectMenu.DefaultValue.user(it.user.id) }
                                ).build()
                        ),
                        Label.of(
                            "Max missing optional participants",
                            StringSelectMenu.create("maxMissingOptional")
                                .setMinValues(1)
                                .setMaxValues(1)
                                .addOptions((0..4).map(Int::toString).map { SelectOption.of(it, it) })
                                .setDefaultValues(activity.maxNumMissingOptionalMembers.toString())
                                .build()
                        ),
                        Label.of(
                            "Voice channel (optional)",
                            EntitySelectMenu.create("voiceChannel", SelectTarget.CHANNEL)
                                .setChannelTypes(ChannelType.VOICE)
                                .setRequired(false)
                                .setMinValues(0)
                                .setMaxValues(1)
                                .also { builder ->
                                    activity.voiceChannel?.let {
                                        builder.setDefaultValues(EntitySelectMenu.DefaultValue.channel(it.id))
                                    }
                                }
                                .build()
                        )
                    )
                    .build()
            }

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(tenant = screen.tenant) { configuration ->
                        val activity = configuration.getActivity(activityId)
                        activity.name = event.getValue("name")!!.asString
                        activity.setRequiredMembers(event.getValue("requiredParticipants")!!.asLongList.map(::DiscordUserId))
                        activity.setOptionalMembers(event.getValue("optionalParticipants")!!.asLongList.map(::DiscordUserId))
                        activity.maxNumMissingOptionalMembers =
                            event.getValue("maxMissingOptional")!!.asStringList.first().toInt()
                        activity.voiceChannel =
                            event.getValue("voiceChannel")!!.asLongList.firstOrNull()?.let(::DiscordVoiceChannelId)
                    }
                }
            }
        }
    }
}
