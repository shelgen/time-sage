package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.screens.AbstractDateRangeScreen
import com.github.shelgen.timesage.ui.screens.CustomIdSerialization
import com.github.shelgen.timesage.ui.screens.ScreenButton
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.Instant
import kotlin.collections.get

object TimeSlotContainerRenderer {
    fun renderTimeSlotContainers(
        weekTimeSlots: List<Instant>,
        dateRange: DateRange,
        tenant: Tenant,
        toggleButtonFactory: (timeSlot: Instant) -> ToggleAvailabilityButton,
    ): List<Container> =
        weekTimeSlots.map {
            renderTimeSlotContainer(
                it,
                PlanningProcessRepository.loadOrInitialize(dateRange, tenant),
                toggleButtonFactory
            )
        }

    private fun renderTimeSlotContainer(
        timeSlot: Instant,
        data: PlanningProcess,
        toggleButtonFactory: (timeSlot: Instant) -> ToggleAvailabilityButton,
    ) = Container.of(
        Section.of(
            toggleButtonFactory(timeSlot).render()
                .let { if (data.concluded) it.asDisabled() else it },
            TextDisplay.of(
                "### ${DiscordFormatter.timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)}\n" +
                        data.availabilityResponses.map
                            .asSequence()
                            .filter { (_, response) -> response.sessionLimit != 0 }
                            .mapNotNull { (userId, response) ->
                                response.dates[timeSlot]?.let { userId to it }
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

    abstract class ToggleAvailabilityButton(
        private val timeSlot: Instant,
        override val screen: AbstractDateRangeScreen
    ) : ScreenButton {
        fun render() =
            Button.primary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+2705"))

        override fun handle(event: ButtonInteractionEvent) {
            event.processAndRerender {
                val userId = event.user.idLong
                PlanningProcessRepository.update(screen.dateRange, screen.tenant) { period ->
                    val old = period.availabilityResponses[userId]?.dates?.get(timeSlot)
                    val new = cycleAvailability(old)
                    logger.info("Updating availability at $timeSlot from $old to $new")
                    period.setAvailability(userId, timeSlot, new)
                }
            }
        }
    }

    private fun cycleAvailability(current: AvailabilityStatus?) = when (current) {
        null -> AvailabilityStatus.AVAILABLE
        AvailabilityStatus.AVAILABLE -> AvailabilityStatus.IF_NEED_BE
        AvailabilityStatus.IF_NEED_BE -> AvailabilityStatus.UNAVAILABLE
        AvailabilityStatus.UNAVAILABLE -> AvailabilityStatus.AVAILABLE
    }
}
