package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import java.time.Instant

class AvailabilityMessageScreen(dateRange: DateRange, tenant: Tenant) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, configuration.tenant)
            ?: error("Planning process for $dateRange in ${configuration.tenant} does not exist!")
        return AvailabilityThreadStartRenderer.renderComponents(dateRange, configuration) +
                PeriodLevelRenderer.renderComponents(
                    dateRange = dateRange,
                    configuration = configuration,
                    toggleSessionLimitButtonFactory = { Buttons.ToggleSessionLimit(this@AvailabilityMessageScreen) },
                ) +
                TimeSlotContainerRenderer.renderTimeSlotContainers(
                    timeSlots = planningProcess.timeSlots,
                    dateRange = dateRange,
                    tenant = tenant
                ) { timeSlot -> Buttons.ToggleTimeSlotAvailability(timeSlot, this) } +
                FooterRenderer.renderComponents { Buttons.PreviewAlternatives(this@AvailabilityMessageScreen) }
    }

    class Buttons {
        class ToggleTimeSlotAvailability(timeSlot: Instant, screen: AvailabilityMessageScreen) :
            TimeSlotContainerRenderer.ToggleAvailabilityButton(timeSlot, screen)

        class ToggleSessionLimit(override val screen: AvailabilityMessageScreen) :
            PeriodLevelRenderer.ToggleSessionLimitButton<AvailabilityMessageScreen>(screen)

        class PreviewAlternatives(override val screen: AvailabilityMessageScreen) :
            FooterRenderer.PreviewAlternativesButton<AvailabilityMessageScreen>(screen)
    }
}
