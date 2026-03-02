package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.*
import com.github.shelgen.timesage.formatAsShortDate
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.ui.DiscordFormatter.timestamp
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Unified availability screen for all target periods (weekly, monthly, or custom).
 *
 * [periodStart] and [periodEnd] define the target period being planned.
 * [pageIndex] controls the layout:
 *   - [SINGLE_PAGE]: single-page message showing all sections.
 *   - 0: thread intro page (concluded banner, missing responses, session limits).
 *   - ≥ 1: thread week page for the week chunk at [pageIndex] - 1.
 * [mainChannelId] — the channel where availability data is stored; for thread pages
 *                   this is the parent channel rather than the thread channel.
 */
class AvailabilityScreen(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val pageIndex: Int,
    val mainChannelId: Long,
    context: OperationContext
) : Screen(OperationContext(guildId = context.guildId, channelId = mainChannelId)) {

    private fun correspondingYearMonth(): YearMonth? {
        val ym = YearMonth.from(periodStart)
        return if (ym.atDay(1) == periodStart && ym.atEndOfMonth() == periodEnd) ym else null
    }

    private fun periodWord() = when {
        correspondingYearMonth() != null -> "month"
        periodEnd == periodStart.plusDays(6) -> "week"
        else -> "period"
    }

    private fun periodLabel() = correspondingYearMonth()
        ?.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        ?: "${periodStart.formatAsShortDate()} through ${periodEnd.formatAsShortDate()}"

    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val data = loadData()
        return when {
            pageIndex == SINGLE_PAGE -> renderSinglePage(data, configuration)
            pageIndex == 0 -> renderIntro(data, configuration)
            else -> renderWeekPage(data, configuration)
        }
    }

    private data class AvailabilityData(
        val responses: UserResponses,
        val concluded: Boolean,
        val conclusionMessageId: Long?,
    )

    private fun loadData(): AvailabilityData =
        AvailabilitiesPeriodRepository.loadOrInitialize(DatePeriod(periodStart, periodEnd), context)
            .let { AvailabilityData(it.responses, it.concluded, it.conclusionMessageId) }

    /**
     * Single-page layout showing all sections in one message.
     * Sections are ordered the same way as in a thread: missing responses and session limits
     * appear before the time slots (mirroring the intro page preceding the week pages).
     */
    private fun renderSinglePage(
        data: AvailabilityData,
        configuration: Configuration,
    ): List<MessageTopLevelComponent> {
        val timeSlots = configuration.scheduling.getTimeSlots(DatePeriod(periodStart, periodEnd), configuration.timeZone)
        return listOf(
            listOf(
                TextDisplay.of(
                    "## Availabilities for ${periodLabel()}\n" +
                            if (!data.concluded) {
                                "Please use the buttons below to toggle your availability" +
                                        formatActivities(configuration.activities)
                            } else {
                                DiscordFormatter.bold(
                                    "✅ Planning for this ${periodWord()} has been concluded" +
                                            data.conclusionMessageId?.let {
                                                "\nSee https://discord.com/channels/${context.guildId}/${context.channelId}/$it"
                                            }.orEmpty()
                                )
                            }
                )
            ),
            renderMissingResponses(data, configuration),
            renderLimits("Limits this ${periodWord()}", data),
            timeSlots.map { renderTimeSlotContainer(it, data) },
        ).flatten()
    }

    /** Thread intro page: concluded banner, missing responses, session limits. */
    private fun renderIntro(
        data: AvailabilityData,
        configuration: Configuration,
    ): List<MessageTopLevelComponent> =
        listOfNotNull(
            if (data.concluded) Container.of(
                TextDisplay.of(
                    DiscordFormatter.bold(
                        "✅ Planning for this ${periodWord()} has been concluded" +
                                data.conclusionMessageId?.let {
                                    "\nSee https://discord.com/channels/${context.guildId}/${context.channelId}/$it"
                                }.orEmpty()
                    )
                )
            ) else null
        ) +
                renderMissingResponses(data, configuration) +
                renderLimits("Limits this ${periodWord()}", data)

    /** Thread week page: week label followed by its time slot containers. */
    private fun renderWeekPage(
        data: AvailabilityData,
        configuration: Configuration,
    ): List<MessageTopLevelComponent> {
        val period = DatePeriod(periodStart, periodEnd)
        val chunks = weekChunks(period, configuration.scheduling.startDayOfWeek)
        val chunk = chunks.getOrElse(pageIndex - 1) { emptyList() }
        val allTimeSlots = configuration.scheduling.getTimeSlots(period, configuration.timeZone)
        val weekTimeSlots = allTimeSlots.filter { slot ->
            slot.atZone(configuration.timeZone.toZoneId()).toLocalDate() in chunk
        }
        val ym = correspondingYearMonth()
        val label = if (ym != null) {
            val monthName = ym.format(DateTimeFormatter.ofPattern("MMMM", Locale.US))
            "$monthName ${chunk.first().dayOfMonth}–${chunk.last().dayOfMonth}"
        } else {
            "${chunk.first().formatAsShortDate()}–${chunk.last().formatAsShortDate()}"
        }
        return listOf(TextDisplay.of("### $label")) +
                weekTimeSlots.map { renderTimeSlotContainer(it, data) }
    }

    private fun renderTimeSlotContainer(timeSlot: Instant, data: AvailabilityData) = Container.of(
        Section.of(
            Buttons.ToggleTimeSlotAvailability(timeSlot = timeSlot, screen = this@AvailabilityScreen).render()
                .let { if (data.concluded) it.asDisabled() else it },
            TextDisplay.of(
                "### ${timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)}\n" +
                        data.responses.map
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

    private fun renderLimits(title: String, data: AvailabilityData) =
        listOf(
            Container.of(
                Section.of(
                    Buttons.ToggleSessionLimit(screen = this@AvailabilityScreen).render()
                        .let { if (data.concluded) it.asDisabled() else it },
                    TextDisplay.of(
                        "### $title\n" +
                                listOf(
                                    data.responses.map
                                        .asSequence()
                                        .mapNotNull { (userId, response) -> response.sessionLimit?.let { userId to it } }
                                        .filter { (_, sessionLimit) -> sessionLimit == 1 }
                                        .map { (userId, _) -> userId }
                                        .sorted()
                                        .toList()
                                        .takeUnless { it.isEmpty() }
                                        ?.joinToString(
                                            prefix = DiscordFormatter.bold("Only one session") + "\n",
                                            transform = DiscordFormatter::mentionUser,
                                            separator = "\n"
                                        ).orEmpty(),
                                    data.responses.map
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

    private fun renderMissingResponses(data: AvailabilityData, configuration: Configuration) =
        listOfNotNull(
            configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .filter { data.responses.forUserId(it) == null }
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

    class Buttons {
        class ToggleTimeSlotAvailability(
            private val timeSlot: Instant,
            override val screen: AvailabilityScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+2705"))

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    AvailabilitiesPeriodRepository.update(
                        period = DatePeriod(screen.periodStart, screen.periodEnd),
                        context = screen.context
                    ) { period ->
                        val old = period.responses.forUserId(userId)?.availabilities?.forTimeSlot(timeSlot)
                        val new = cycleAvailability(old)
                        logger.info("Updating availability at $timeSlot from $old to $new")
                        period.setUserTimeSlotAvailability(userId, timeSlot, new)
                    }
                }
            }
        }

        class ToggleSessionLimit(override val screen: AvailabilityScreen) : ScreenButton {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+1F6AB"))

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    AvailabilitiesPeriodRepository.update(
                        period = DatePeriod(screen.periodStart, screen.periodEnd),
                        context = screen.context
                    ) { period ->
                        val old = period.responses.forUserId(userId)?.sessionLimit
                        val new = cycleLimit(old)
                        logger.info("Updating session limit for period ${screen.periodStart}–${screen.periodEnd} from $old to $new")
                        period.setUserSessionLimit(userId, new)
                    }
                }
            }
        }
    }

    companion object {
        const val SINGLE_PAGE = -1

        private fun cycleAvailability(current: AvailabilityStatus?) = when (current) {
            null -> AvailabilityStatus.AVAILABLE
            AvailabilityStatus.AVAILABLE -> AvailabilityStatus.IF_NEED_BE
            AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.UNAVAILABLE
            AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.AVAILABLE
        }

        private fun cycleLimit(current: Int?) = when (current) {
            0 -> 2
            1 -> 0
            else -> 1
        }

        /**
         * Splits the dates of [period] into week chunks, each starting on [startDayOfWeek].
         * The first chunk may be a partial week and the last chunk may also be partial.
         */
        fun weekChunks(period: DatePeriod, startDayOfWeek: DayOfWeek): List<List<LocalDate>> {
            val result = mutableListOf<MutableList<LocalDate>>()
            var current = mutableListOf<LocalDate>()
            for (date in period.dates()) {
                if (date.dayOfWeek == startDayOfWeek && current.isNotEmpty()) {
                    result.add(current)
                    current = mutableListOf()
                }
                current.add(date)
            }
            if (current.isNotEmpty()) result.add(current)
            return result
        }
    }
}
