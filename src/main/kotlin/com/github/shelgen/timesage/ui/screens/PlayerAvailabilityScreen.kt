package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.atNormalStartTime
import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Week
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.ui.DiscordFormatter.timestamp
import com.github.shelgen.timesage.weekDatesForMonday
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class PlayerAvailabilityScreen(val weekMondayDate: LocalDate, guildId: Long) : Screen(guildId) {
    override fun renderComponents() =
        WeekRepository.loadOrInitialize(guildId = guildId, mondayDate = weekMondayDate).let { week ->
            val dates = weekDatesForMonday(weekMondayDate)
            listOf(
                renderHeader(dates.first(), dates.last()),
                dates.map { date -> renderDateContainer(date, week) },
                renderWeekLimits(week),
                renderMissingResponses(week)
            ).flatten()
        }

    private fun renderHeader(from: LocalDate, to: LocalDate) = listOf(
        TextDisplay.of(
            "## Availabilities for ${from.formatAsShortDate()}" +
                    " through ${to.formatAsShortDate()}"
        )
    )

    private fun renderDateContainer(date: LocalDate, week: Week) = Container.of(
        Section.of(
            Buttons.ToggleDateAvailability(date = date, screen = this@PlayerAvailabilityScreen).render(),
            TextDisplay.of(
                "### ${
                    timestamp(
                        date.atNormalStartTime(),
                        DiscordFormatter.TimestampFormat.LONG_DATE_TIME
                    )
                }\n" +
                        week
                            .playerResponses
                            .asSequence()
                            .filter { (_, response) -> response.sessionLimit != 0 }
                            .mapNotNull { (userId, response) -> response.availability[date]?.let { userId to it } }
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

    private fun renderWeekLimits(week: Week) =
        listOf(
            Container.of(
                Section.of(
                    Buttons.ToggleWeekSessionLimit(screen = this@PlayerAvailabilityScreen).render(),
                    TextDisplay.of(
                        "### Limits this week\n" +
                                listOf(
                                    week
                                        .playerResponses
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
                                        .playerResponses
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

    private fun renderMissingResponses(week: Week) =
        listOfNotNull(
            configuration.campaigns
                .flatMap { it.gmDiscordIds + it.playerDiscordIds }
                .filter { week.playerResponses[it] == null }
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

    override fun parameters(): List<String> = listOf(weekMondayDate.toString())

    companion object {
        fun reconstruct(parameters: List<String>, guildId: Long) =
            PlayerAvailabilityScreen(weekMondayDate = LocalDate.parse(parameters.first()), guildId)
    }

    class Buttons {
        class ToggleDateAvailability(private val date: LocalDate, screen: PlayerAvailabilityScreen) :
            ScreenButton<PlayerAvailabilityScreen>(
                style = ButtonStyle.PRIMARY,
                label = null,
                emoji = Emoji.fromUnicode("U+2705"),
                screen = screen
            ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    WeekRepository.update(guildId = screen.guildId, mondayDate = screen.weekMondayDate) { week ->
                        val playerResponse = week.playerResponses[userId]
                        val oldAvailability = playerResponse?.availability[date]
                        val newAvailability = when (oldAvailability) {
                            null -> AvailabilityStatus.AVAILABLE
                            AvailabilityStatus.AVAILABLE -> AvailabilityStatus.IF_NEED_BE
                            AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.UNAVAILABLE
                            AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.AVAILABLE
                        }
                        logger.info("Updating player availability for ${event.user.name} from $oldAvailability to $newAvailability")
                        if (playerResponse == null) {
                            week.playerResponses[userId] = WeekRepository.MutableWeek.PlayerResponse(
                                sessionLimit = null,
                                availability = mutableMapOf(date to newAvailability)
                            )
                        } else {
                            playerResponse.availability[date] = newAvailability
                        }
                    }
                }
            }

            override fun parameters(): List<String> = listOf(date.toString())

            object Reconstructor :
                ScreenComponentReconstructor<PlayerAvailabilityScreen, ToggleDateAvailability>(
                    screenClass = PlayerAvailabilityScreen::class,
                    componentClass = ToggleDateAvailability::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    ToggleDateAvailability(
                        date = LocalDate.parse(componentParameters.first()),
                        screen = reconstruct(parameters = screenParameters, guildId = guildId)
                    )
            }
        }

        class ToggleWeekSessionLimit(screen: PlayerAvailabilityScreen) :
            ScreenButton<PlayerAvailabilityScreen>(
                style = ButtonStyle.SECONDARY,
                label = null,
                emoji = Emoji.fromUnicode("U+1F6AB"),
                screen = screen
            ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    WeekRepository.update(guildId = screen.guildId, mondayDate = screen.weekMondayDate) { week ->
                        val playerResponse = week.playerResponses[userId]
                        val oldLimit = playerResponse?.sessionLimit
                        val newLimit = when (oldLimit) {
                            0 -> 2
                            1 -> 0
                            else -> 1
                        }
                        logger.info("Setting session limit for ${event.user.name} at week of ${screen.weekMondayDate} to $newLimit")
                        if (playerResponse == null) {
                            week.playerResponses[userId] = WeekRepository.MutableWeek.PlayerResponse(
                                sessionLimit = newLimit,
                                availability = mutableMapOf()
                            )
                        } else {
                            playerResponse.sessionLimit = newLimit
                        }
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<PlayerAvailabilityScreen, ToggleWeekSessionLimit>(
                    screenClass = PlayerAvailabilityScreen::class,
                    componentClass = ToggleWeekSessionLimit::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    ToggleWeekSessionLimit(
                        screen = reconstruct(parameters = screenParameters, guildId = guildId)
                    )
            }
        }
    }
}

fun LocalDate.formatAsShortDate(): String = format(DateTimeFormatter.ofPattern("LLLL d", Locale.US))
