package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

class AvailabilityThreadWeekScreen(
    val weekChunkIndex: Int,
    dateRange: DateRange,
    tenant: Tenant
) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val weekChunk =
            dateRange.chunkedByWeek(configuration.localization.startDayOfWeek)
                .getOrElse(weekChunkIndex) { emptyList() }
        val allTimeSlots =
            configuration.scheduling.timeSlotRules.getTimeSlots(dateRange, configuration.localization.timeZone)
        val weekTimeSlots = allTimeSlots.filter { slot ->
            slot.atZone(configuration.localization.timeZone.toZoneId()).toLocalDate() in weekChunk
        }
        return TimeSlotContainerRenderer.renderTimeSlotContainers(
            weekTimeSlots = weekTimeSlots,
            dateRange = dateRange,
            tenant = tenant
        ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }
    }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityThreadWeekScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)
    }
}
