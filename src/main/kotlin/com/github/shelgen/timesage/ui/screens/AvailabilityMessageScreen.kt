package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

class AvailabilityMessageScreen(targetPeriod: TargetPeriod, tenant: Tenant) : AbstractTargetPeriodScreen(targetPeriod, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        AvailabilityThreadStartScreen(targetPeriod, tenant).renderComponents(configuration) +
                PeriodLevelScreenContent<AvailabilityMessageScreen>(this) {
                    Buttons.ToggleSessionLimit(this)
                }.renderComponents(configuration) +
                TimeSlotContainerRenderer.renderTimeSlotContainers(
                    weekTimeSlots = configuration.scheduling.timeSlotRules.getTimeSlots(
                        dateRange = targetPeriod,
                        timeZone = configuration.localization.timeZone
                    ),
                    targetPeriod = targetPeriod,
                    tenant = tenant
                ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityMessageScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)

        class ToggleSessionLimit(override val screen: AvailabilityMessageScreen) :
            PeriodLevelScreenContent.ToggleSessionLimitButton<AvailabilityMessageScreen>(screen)
    }
}
