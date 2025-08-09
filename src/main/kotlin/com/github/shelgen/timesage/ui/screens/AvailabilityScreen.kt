package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.*
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.ui.DiscordFormatter.timestamp
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class AvailabilityScreen(val startDate: LocalDate, context: OperationContext) : Screen(context) {
    override fun renderComponents(configuration: Configuration) =
        AvailabilitiesWeekRepository.loadOrInitialize(startDate = startDate, context = context).let { week ->
            val datePeriod = DatePeriod.weekFrom(startDate)
            val timeSlots = configuration.scheduling.getTimeSlots(datePeriod)
            listOf(
                renderHeader(datePeriod.fromDate, datePeriod.toDate, configuration),
                timeSlots.map { timeSlot -> renderTimeSlotContainer(timeSlot, week) },
                renderWeekLimits(week),
                renderMissingResponses(week, configuration)
            ).flatten()
        }

    private fun renderHeader(from: LocalDate, to: LocalDate, configuration: Configuration): List<TextDisplay> {
        return listOf(
            TextDisplay.of(
                "## Availabilities for ${from.formatAsShortDate()} through ${to.formatAsShortDate()}\n" +
                        "Please use the buttons below to toggle your availability" +
                        formatActivities(configuration.activities)
            )
        )
    }

    private fun formatActivities(activities: List<Activity>) =
        when (activities.size) {
            0 -> ""
            1 -> " for\n${DiscordFormatter.bold(activities.first().name)}"
            else -> " for one or more of\n" +
                    activities.map(Activity::name)
                        .sorted()
                        .map(DiscordFormatter::bold)
                        .joinToString("\n") { "- $it" }
        }

    private fun renderTimeSlotContainer(timeSlot: Instant, week: AvailabilitiesWeek) = Container.of(
        Section.of(
            Buttons.ToggleTimeSlotAvailability(timeSlot = timeSlot, screen = this@AvailabilityScreen).render(),
            TextDisplay.of(
                "### ${timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)}\n" +
                        week
                            .responses
                            .map
                            .asSequence()
                            .filter { (_, response) -> response.sessionLimit != 0 }
                            .mapNotNull { (userId, response) ->
                                response.availabilities.forTimeSlot(timeSlot)?.let { userId to it }
                            }
                            .filter { (_, availability) ->
                                availability in setOf(
                                    AvailabilityStatus.AVAILABLE,
                                    AvailabilityStatus.IF_NEED_BE
                                )
                            }
                            .toList()
                            .sortedBy { (userId, _) -> userId }
                            .takeUnless { it.isEmpty() }
                            ?.joinToString(separator = "\n") { (userId, availability) ->
                                if (availability == AvailabilityStatus.IF_NEED_BE) {
                                    DiscordFormatter.italics("${DiscordFormatter.mentionUser(userId)} (if need be)")
                                } else {
                                    DiscordFormatter.bold(DiscordFormatter.mentionUser(userId))
                                }
                            }
                            .orEmpty()
            )
        )
    )

    private fun renderWeekLimits(week: AvailabilitiesWeek) =
        listOf(
            Container.of(
                Section.of(
                    Buttons.ToggleWeekSessionLimit(screen = this@AvailabilityScreen).render(),
                    TextDisplay.of(
                        "### Limits this week\n" +
                                listOf(
                                    week
                                        .responses
                                        .map
                                        .asSequence()
                                        .mapNotNull { (userId, response) -> response.sessionLimit?.let { userId to it } }
                                        .filter { (_, sessionLimit) -> sessionLimit == 1 }
                                        .map { (userId, _) -> userId }
                                        .sorted()
                                        .toList()
                                        .takeUnless { it.isEmpty() }?.joinToString(
                                            prefix = DiscordFormatter.bold("Only one session") + "\n",
                                            transform = DiscordFormatter::mentionUser,
                                            separator = "\n"
                                        ).orEmpty(),
                                    week
                                        .responses
                                        .map
                                        .asSequence()
                                        .mapNotNull { (userId, response) -> response.sessionLimit?.let { userId to it } }
                                        .filter { (_, sessionLimit) -> sessionLimit == 0 }
                                        .map { (userId, _) -> userId }
                                        .sorted()
                                        .toList()
                                        .takeUnless { it.isEmpty() }
                                        ?.joinToString(
                                            prefix = DiscordFormatter.bold("Can't make it at all") + "\n",
                                            transform = DiscordFormatter::mentionUser,
                                            separator = "\n"
                                        ).orEmpty()
                                ).joinToString(separator = "\n\n")
                    )
                )
            )
        )

    private fun renderMissingResponses(week: AvailabilitiesWeek, configuration: Configuration) =
        listOfNotNull(
            configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .filter { week.responses.forUserId(it) == null }
                .distinct()
                .sorted()
                .takeUnless(List<Long>::isEmpty)
                ?.joinToString(
                    prefix = "### Missing responses\n",
                    separator = "\n"
                ) { userId -> DiscordFormatter.mentionUser(userId) }
                ?.let(TextDisplay::of)
                ?.let { Container.of(it).withAccentColor(0xFFB6C1) }
        )

    override fun parameters(): List<String> = listOf(startDate.toString())

    companion object {
        fun reconstruct(parameters: List<String>, context: OperationContext) =
            AvailabilityScreen(startDate = LocalDate.parse(parameters.first()), context)
    }

    class Buttons {
        class ToggleTimeSlotAvailability(private val timeSlot: Instant, screen: AvailabilityScreen) :
            ScreenButton<AvailabilityScreen>(
                screen = screen
            ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+2705"))

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    AvailabilitiesWeekRepository.update(
                        startDate = screen.startDate,
                        context = screen.context
                    ) { week ->
                        val oldAvailability = week.responses.forUserId(userId)?.availabilities?.forTimeSlot(timeSlot)
                        val newAvailability = when (oldAvailability) {
                            null -> AvailabilityStatus.AVAILABLE
                            AvailabilityStatus.AVAILABLE -> AvailabilityStatus.IF_NEED_BE
                            AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.UNAVAILABLE
                            AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.AVAILABLE
                        }
                        logger.info("Updating availability at $timeSlot from $oldAvailability to $newAvailability")
                        week.setUserTimeSlotAvailability(
                            userId = userId,
                            timeSlot = timeSlot,
                            availabilityStatus = newAvailability
                        )
                    }
                }
            }

            override fun parameters(): List<String> = listOf(timeSlot.toString())

            object Reconstructor :
                ScreenComponentReconstructor<AvailabilityScreen, ToggleTimeSlotAvailability>(
                    screenClass = AvailabilityScreen::class,
                    componentClass = ToggleTimeSlotAvailability::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    ToggleTimeSlotAvailability(
                        timeSlot = Instant.parse(componentParameters.first()),
                        screen = reconstruct(screenParameters, context)
                    )
            }
        }

        class ToggleWeekSessionLimit(screen: AvailabilityScreen) :
            ScreenButton<AvailabilityScreen>(
                screen = screen
            ) {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+1F6AB"))

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    AvailabilitiesWeekRepository.update(
                        startDate = screen.startDate,
                        context = screen.context
                    ) { week ->
                        val oldLimit = week.responses.forUserId(userId)?.sessionLimit
                        val newLimit = when (oldLimit) {
                            0 -> 2
                            1 -> 0
                            else -> 1
                        }
                        logger.info("Updating session limit at week of ${screen.startDate} from $oldLimit to $newLimit")
                        week.setUserSessionLimit(userId, newLimit)
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<AvailabilityScreen, ToggleWeekSessionLimit>(
                    screenClass = AvailabilityScreen::class,
                    componentClass = ToggleWeekSessionLimit::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) = ToggleWeekSessionLimit(screen = reconstruct(screenParameters, context))
            }
        }
    }
}

fun LocalDate.formatAsShortDate(): String = format(DateTimeFormatter.ofPattern("LLLL d", Locale.US))
