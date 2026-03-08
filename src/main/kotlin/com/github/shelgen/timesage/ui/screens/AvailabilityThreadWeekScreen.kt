package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.ui.TimeSlotContainerRenderer
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
                .getOrElse(weekChunkIndex) { return@renderComponents emptyList() }
        val weekTimeSlots = configuration.produceTimeSlots(dateRange).filter { slot ->
            configuration.localization.dateOf(slot) in weekChunk
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
