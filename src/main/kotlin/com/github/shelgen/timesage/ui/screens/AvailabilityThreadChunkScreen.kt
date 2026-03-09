package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

private const val CHUNK_SIZE = 7

class AvailabilityThreadChunkScreen(
    val chunkIndex: Int,
    dateRange: DateRange,
    tenant: Tenant
) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, configuration.tenant)
            ?: return planningProcessNotFound()
        val chunkTimeSlots = planningProcess.timeSlots
            .chunked(CHUNK_SIZE)
            .getOrElse(chunkIndex) { emptyList() }
        if (chunkTimeSlots.isEmpty()) return planningProcessNotFound()
        return TimeSlotContainerRenderer.renderTimeSlotContainers(
            timeSlots = chunkTimeSlots,
            dateRange = dateRange,
            tenant = tenant
        ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }
    }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityThreadChunkScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)
    }
}
