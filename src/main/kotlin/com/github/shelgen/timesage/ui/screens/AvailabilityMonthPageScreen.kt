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
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/** Max number of time slots shown per thread message to stay within Discord's 40-component limit. */
private const val SLOTS_PER_PAGE = 12

/**
 * One page of monthly availability shown in the thread. Fields [yearMonth], [pageIndex], and
 * [mainChannelId] are serialized into button custom IDs. The screen overrides [context] to always
 * use [mainChannelId] so data lookups target the parent channel even when the interaction arrives
 * from inside the thread.
 */
class AvailabilityMonthPageScreen(
    val yearMonth: YearMonth,
    val pageIndex: Int,
    val mainChannelId: Long,
    context: OperationContext
) : Screen(OperationContext(guildId = context.guildId, channelId = mainChannelId)) {

    override fun renderComponents(configuration: Configuration) =
        AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = this.context).let { month ->
            val datePeriod = DatePeriod.monthFrom(yearMonth)
            val allTimeSlots = configuration.scheduling.getTimeSlots(datePeriod, configuration.timeZone)
            val pageTimeSlots = allTimeSlots.drop(pageIndex * SLOTS_PER_PAGE).take(SLOTS_PER_PAGE)
            val isFirstPage = pageIndex == 0
            val isLastPage = (pageIndex + 1) * SLOTS_PER_PAGE >= allTimeSlots.size

            listOf(
                if (isFirstPage) renderHeader(month, configuration) else emptyList(),
                pageTimeSlots.map { timeSlot -> renderTimeSlotContainer(timeSlot, month) },
                if (isFirstPage) renderMonthLimits(month) else emptyList(),
                if (isLastPage) renderMissingResponses(month, configuration) else emptyList()
            ).flatten()
        }

    private fun renderHeader(
        month: AvailabilitiesMonth,
        configuration: Configuration
    ) = listOf(
        TextDisplay.of(
            "## Availabilities for ${yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))}\n" +
                    if (!month.concluded) {
                        "Please use the buttons below to toggle your availability" +
                                formatActivities(configuration.activities)
                    } else {
                        DiscordFormatter.bold(
                            "✅ Planning for this month has been concluded" +
                                    month.conclusionMessageId?.let {
                                        "\nSee https://discord.com/channels/${this.context.guildId}/${this.context.channelId}/$it"
                                    }.orEmpty()
                        )
                    }
        )
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
        fun slotsPerPage() = SLOTS_PER_PAGE
    }
}
