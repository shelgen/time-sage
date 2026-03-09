package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.planning.Availability
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.Instant

object TimeSlotContainerRenderer {
    fun renderTimeSlotContainers(
        timeSlots: List<Instant>,
        dateRange: DateRange,
        tenant: Tenant,
        toggleButtonFactory: (timeSlot: Instant) -> ToggleAvailabilityButton,
    ): List<Container> =
        timeSlots.map {
            renderTimeSlotContainer(
                it,
                PlanningProcessRepository.load(dateRange, tenant)!!,
                toggleButtonFactory
            )
        }

    private fun renderTimeSlotContainer(
        timeSlot: Instant,
        planningProcess: PlanningProcess,
        toggleButtonFactory: (timeSlot: Instant) -> ToggleAvailabilityButton,
    ) = Container.of(
        Section.of(
            toggleButtonFactory(timeSlot).render()
                .let { if (planningProcess.isLocked()) it.asDisabled() else it },
            TextDisplay.of(
                "### ${DiscordFormatter.timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)}\n" +
                        planningProcess.availabilityResponses
                            .asSequence()
                            .filter { (_, response) -> response.sessionLimit != 0 }
                            .mapNotNull { (userId, response) ->
                                response[timeSlot]?.let { userId to it }
                            }
                            .filter { (_, availability) ->
                                availability in setOf(
                                    Availability.AVAILABLE,
                                    Availability.IF_NEED_BE
                                )
                            }
                            .toList()
                            .sortedBy { (userId, _) -> userId.id }
                            .takeUnless { it.isEmpty() }
                            ?.joinToString(separator = "\n") { (userId, availability) ->
                                if (availability == Availability.IF_NEED_BE) {
                                    DiscordFormatter.italics("${userId.toMention()} (if need be)")
                                } else {
                                    DiscordFormatter.bold(userId.toMention())
                                }
                            }
                            .orEmpty()
            )
        )
    )

    abstract class ToggleAvailabilityButton(
        val timeSlot: Instant,
        override val screen: AbstractDateRangeScreen
    ) : ScreenButton {
        fun render() =
            Button.primary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+2705"))

        override fun handle(event: ButtonInteractionEvent) {
            event.processAndRerender {
                val userId = DiscordUserId(event.user.idLong)
                val planSessionLimit = ConfigurationRepository.loadOrInitialize(screen.tenant).sessionLimit
                val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                PlanningProcessRepository.update(planningProcess) { period ->
                    val old = period.availabilityResponses[userId]?.get(timeSlot)
                    val new = cycleAvailability(old)
                    logger.info("Updating availability at $timeSlot from $old to $new")
                    period.setAvailability(userId, timeSlot, new, planSessionLimit)
                }
            }
        }
    }

    private fun cycleAvailability(current: Availability?) = when (current) {
        null -> Availability.AVAILABLE
        Availability.AVAILABLE -> Availability.IF_NEED_BE
        Availability.IF_NEED_BE -> Availability.UNAVAILABLE
        Availability.UNAVAILABLE -> Availability.AVAILABLE
    }
}
