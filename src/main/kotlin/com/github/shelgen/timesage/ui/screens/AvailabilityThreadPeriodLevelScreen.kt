package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
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
            PeriodLevelRenderer.ToggleSessionLimitButton<AvailabilityThreadPeriodLevelScreen>(screen)

        class PreviewAlternatives(override val screen: AvailabilityThreadPeriodLevelScreen) :
            PeriodLevelRenderer.PreviewAlternativesButton<AvailabilityThreadPeriodLevelScreen>(screen)
    }
}
