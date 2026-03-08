package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

class AvailabilityMessageScreen(dateRange: DateRange, tenant: Tenant) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        AvailabilityThreadStartRenderer.renderComponents(dateRange, configuration) +
                PeriodLevelRenderer.renderComponents(dateRange, configuration) {
                    Buttons.ToggleSessionLimit(this@AvailabilityMessageScreen)
                } +
                TimeSlotContainerRenderer.renderTimeSlotContainers(
                    weekTimeSlots = configuration.produceTimeSlots(dateRange),
                    dateRange = dateRange,
                    tenant = tenant
                ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityMessageScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)

        class ToggleSessionLimit(override val screen: AvailabilityMessageScreen) :
            PeriodLevelRenderer.ToggleSessionLimitButton<AvailabilityMessageScreen>(screen)
    }
}
