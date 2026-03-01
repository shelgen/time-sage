package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.*
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesMonthRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.ui.DiscordFormatter.timestamp
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
 * One message in the monthly availability thread. Fields [yearMonth], [pageIndex], and
 * [mainChannelId] are serialized into button custom IDs. The screen overrides [context] to always
 * use [mainChannelId] so data lookups target the parent channel even when the interaction arrives
 * from inside the thread.
 *
 * [pageIndex] == 0 → intro message: concluded banner (if applicable), missing responses, session limits.
 * [pageIndex] >= 1 → week message for week chunk at [pageIndex] - 1, ordered by [Configuration.scheduling]'s start day of week.
 */
class AvailabilityMonthPageScreen(
    val yearMonth: YearMonth,
    val pageIndex: Int,
    val mainChannelId: Long,
    context: OperationContext
) : Screen(OperationContext(guildId = context.guildId, channelId = mainChannelId)) {

    override fun renderComponents(configuration: Configuration) =
        AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = this.context).let { month ->
            if (pageIndex == 0) {
                renderIntro(month, configuration)
            } else {
                val chunks = weekChunks(yearMonth, configuration.scheduling.startDayOfWeek)
                val chunk = chunks.getOrElse(pageIndex - 1) { emptyList() }
                val allTimeSlots = configuration.scheduling.getTimeSlots(
                    DatePeriod.monthFrom(yearMonth), configuration.timeZone
                )
                renderWeek(chunk, allTimeSlots, month, configuration)
            }
        }

    private fun renderIntro(month: AvailabilitiesMonth, configuration: Configuration) =
        listOfNotNull(
            if (month.concluded) Container.of(
                TextDisplay.of(
                    DiscordFormatter.bold(
                        "✅ Planning for this month has been concluded" +
                                month.conclusionMessageId?.let {
                                    "\nSee https://discord.com/channels/${this.context.guildId}/${this.context.channelId}/$it"
                                }.orEmpty()
                    )
                )
            ) else null
        ) +
        renderMissingResponses(month, configuration) +
        renderMonthLimits(month)

    private fun renderWeek(
        chunk: List<LocalDate>,
        allTimeSlots: List<Instant>,
        month: AvailabilitiesMonth,
        configuration: Configuration,
    ): List<net.dv8tion.jda.api.components.MessageTopLevelComponent> {
        val weekTimeSlots = allTimeSlots.filter { slot ->
            slot.atZone(configuration.timeZone.toZoneId()).toLocalDate() in chunk
        }
        val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM", Locale.US))
        val label = "$monthName ${chunk.first().dayOfMonth}–${chunk.last().dayOfMonth}"
        return listOf(TextDisplay.of("### $label")) +
                weekTimeSlots.map { timeSlot -> renderTimeSlotContainer(timeSlot, month) }
    }

    private fun renderTimeSlotContainer(timeSlot: Instant, month: AvailabilitiesMonth) = Container.of(
        Section.of(
            Buttons.ToggleTimeSlotAvailability(timeSlot = timeSlot, screen = this@AvailabilityMonthPageScreen).render()
                .let { if (month.concluded) it.asDisabled() else it },
            TextDisplay.of(
                "### ${timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)}\n" +
                        month
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

    private fun renderMonthLimits(month: AvailabilitiesMonth) =
        listOf(
            Container.of(
                Section.of(
                    Buttons.ToggleMonthSessionLimit(screen = this@AvailabilityMonthPageScreen).render()
                        .let { if (month.concluded) it.asDisabled() else it },
                    TextDisplay.of(
                        "### Limits this month\n" +
                                listOf(
                                    month
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
                                    month
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

    private fun renderMissingResponses(month: AvailabilitiesMonth, configuration: Configuration) =
        listOfNotNull(
            configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .filter { month.responses.forUserId(it) == null }
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

    class Buttons {
        class ToggleTimeSlotAvailability(
            private val timeSlot: Instant,
            override val screen: AvailabilityMonthPageScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+2705"))

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    AvailabilitiesMonthRepository.update(
                        yearMonth = screen.yearMonth,
                        context = screen.context
                    ) { month ->
                        val oldAvailability = month.responses.forUserId(userId)?.availabilities?.forTimeSlot(timeSlot)
                        val newAvailability = when (oldAvailability) {
                            null -> AvailabilityStatus.AVAILABLE
                            AvailabilityStatus.AVAILABLE -> AvailabilityStatus.IF_NEED_BE
                            AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.UNAVAILABLE
                            AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.AVAILABLE
                        }
                        logger.info("Updating availability at $timeSlot from $oldAvailability to $newAvailability")
                        month.setUserTimeSlotAvailability(
                            userId = userId,
                            timeSlot = timeSlot,
                            availabilityStatus = newAvailability
                        )
                    }
                }
            }
        }

        class ToggleMonthSessionLimit(override val screen: AvailabilityMonthPageScreen) : ScreenButton {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+1F6AB"))

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val userId = event.user.idLong
                    AvailabilitiesMonthRepository.update(
                        yearMonth = screen.yearMonth,
                        context = screen.context
                    ) { month ->
                        val oldLimit = month.responses.forUserId(userId)?.sessionLimit
                        val newLimit = when (oldLimit) {
                            0 -> 2
                            1 -> 0
                            else -> 1
                        }
                        logger.info("Updating session limit for ${screen.yearMonth} from $oldLimit to $newLimit")
                        month.setUserSessionLimit(userId, newLimit)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Splits the dates of [yearMonth] into week chunks, each starting on [startDayOfWeek].
         * The first chunk may be a partial week (from the 1st to the day before the first occurrence
         * of [startDayOfWeek]), and the last chunk may also be partial.
         */
        fun weekChunks(yearMonth: YearMonth, startDayOfWeek: DayOfWeek): List<List<LocalDate>> {
            val result = mutableListOf<MutableList<LocalDate>>()
            var current = mutableListOf<LocalDate>()
            for (date in DatePeriod.monthFrom(yearMonth).dates()) {
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

