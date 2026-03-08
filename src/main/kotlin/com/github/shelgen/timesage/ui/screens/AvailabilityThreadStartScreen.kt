package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange

class AvailabilityThreadStartScreen(val dateRange: DateRange, tenant: Tenant) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration) =
        AvailabilityThreadStartRenderer.renderComponents(dateRange, configuration)
}
