package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.Instant

class AvailabilityThreadWeekScreen(
    val weekChunkIndex: Int,
    dateRange: DateRange,
    tenant: Tenant
) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val weekChunk =
            dateRange.chunkedByWeek(configuration.localization.startDayOfWeek)
                .getOrElse(weekChunkIndex) { return@renderComponents emptyList() }
        val planningProcess = PlanningProcessRepository.load(dateRange, configuration.tenant)
            ?: error("Planning process for $dateRange in ${configuration.tenant} does not exist!")
        val weekTimeSlots = planningProcess.timeSlots.filter { slot ->
            configuration.localization.dateOf(slot) in weekChunk
        }
        if (weekTimeSlots.isEmpty()) {
            return if (weekChunkIndex == 0) {
                listOf(TextDisplay.of(DiscordFormatter.italics("-# The first week this period has no time slots.")))
            } else {
                listOf(TextDisplay.of(DiscordFormatter.italics("-# The last week this period has no time slots.")))
            }
        }
        return TimeSlotContainerRenderer.renderTimeSlotContainers(
            timeSlots = weekTimeSlots,
            dateRange = dateRange,
            tenant = tenant
        ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }
    }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityThreadWeekScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)
    }
}
