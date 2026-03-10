package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange

class AvailabilityThreadFooterScreen(dateRange: DateRange, tenant: Tenant) : AbstractDateRangeScreen(dateRange, tenant) {
    override fun renderComponents(configuration: Configuration) =
        FooterRenderer.renderComponents { Buttons.PreviewAlternatives(this@AvailabilityThreadFooterScreen) }

    class Buttons {
        class PreviewAlternatives(override val screen: AvailabilityThreadFooterScreen) :
            FooterRenderer.PreviewAlternativesButton<AvailabilityThreadFooterScreen>(screen)
    }
}
