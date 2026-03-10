package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.AvailabilityThread
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

class AvailabilityThreadTimeSlotChunkScreen(
    val fromInclusive: Int,
    val size: Int,
    dateRange: DateRange,
    tenant: Tenant
) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, configuration.tenant)
            ?: return planningProcessNotFound()
        val timeSlots = planningProcess.timeSlots.drop(fromInclusive).take(size)
        if (timeSlots.isEmpty()) return planningProcessNotFound()
        return TimeSlotContainerRenderer.renderTimeSlotContainers(
            timeSlots = timeSlots,
            dateRange = dateRange,
            tenant = tenant
        ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }
    }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityThreadTimeSlotChunkScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen) {

            override fun onAfterUpdate() {
                val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant) ?: return
                val ai = planningProcess.availabilityInterface as? AvailabilityThread ?: return
                JDAHolder.getThreadChannel(ai.threadChannel)
                    .editMessageById(
                        ai.periodLevelMessage.id,
                        AvailabilityThreadPeriodLevelScreen(screen.dateRange, screen.tenant).renderEdit()
                    )
                    .queue()
            }
        }
    }
}
