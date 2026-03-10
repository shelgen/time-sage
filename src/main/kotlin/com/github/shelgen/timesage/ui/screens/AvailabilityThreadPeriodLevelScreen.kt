package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.AvailabilityThread
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange

class AvailabilityThreadPeriodLevelScreen(dateRange: DateRange, tenant: Tenant) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration) =
        PeriodLevelRenderer.renderComponents(
            dateRange = dateRange,
            configuration = configuration,
            toggleSessionLimitButtonFactory = { Buttons.ToggleSessionLimit(this@AvailabilityThreadPeriodLevelScreen) },
            previewAlternativesButtonFactory = { Buttons.PreviewAlternatives(this@AvailabilityThreadPeriodLevelScreen) },
        )

    class Buttons {
        class ToggleSessionLimit(override val screen: AvailabilityThreadPeriodLevelScreen) :
            PeriodLevelRenderer.ToggleSessionLimitButton<AvailabilityThreadPeriodLevelScreen>(screen) {

            override fun onAfterUpdate() {
                val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant) ?: return
                val ai = planningProcess.availabilityInterface as? AvailabilityThread ?: return
                val threadChannel = JDAHolder.getThreadChannel(ai.threadChannel)
                var timeSlotIndex = 0
                ai.timeSlotChunks.forEach { chunk ->
                    threadChannel
                        .editMessageById(
                            chunk.message.id,
                            AvailabilityThreadTimeSlotChunkScreen(
                                fromInclusive = timeSlotIndex,
                                size = chunk.size,
                                dateRange = screen.dateRange,
                                tenant = screen.tenant
                            ).renderEdit()
                        )
                        .queue()
                    timeSlotIndex += chunk.size
                }
            }
        }

        class PreviewAlternatives(override val screen: AvailabilityThreadPeriodLevelScreen) :
            PeriodLevelRenderer.PreviewAlternativesButton<AvailabilityThreadPeriodLevelScreen>(screen)
    }
}
