package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

class AvailabilityThreadWeekScreen(
    val weekChunkIndex: Int,
    targetPeriod: TargetPeriod,
    tenant: Tenant
) : AbstractTargetPeriodScreen(targetPeriod, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val weekChunk =
            targetPeriod.chunkedByWeek(configuration.localization.startDayOfWeek)
                .getOrElse(weekChunkIndex) { emptyList() }
        val allTimeSlots =
            configuration.scheduling.timeSlotRules.getTimeSlots(targetPeriod, configuration.localization.timeZone)
        val weekTimeSlots = allTimeSlots.filter { slot ->
            slot.atZone(configuration.localization.timeZone.toZoneId()).toLocalDate() in weekChunk
        }
        return TimeSlotContainerRenderer.renderTimeSlotContainers(
            weekTimeSlots = weekTimeSlots,
            targetPeriod = targetPeriod,
            tenant = tenant
        ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }
    }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityThreadWeekScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)
    }
}
