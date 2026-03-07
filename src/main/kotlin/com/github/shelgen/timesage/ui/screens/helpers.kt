package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.formatAsShortDate
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import java.time.format.DateTimeFormatter
import java.util.*

fun getPlans(
    configuration: Configuration,
    dateRange: DateRange,
    tenant: Tenant
): List<Plan> = Planner(
    configuration = configuration,
    dateRange = dateRange,
    responses = AvailabilitiesPeriodRepository.loadOrInitialize(dateRange, tenant).responses
).generatePossiblePlans()

fun getPlan(
    planNumber: Int,
    dateRange: DateRange,
    configuration: Configuration,
    tenant: Tenant
): Plan = if (planNumber == 0) {
    Plan(emptyList())
} else {
    getPlans(configuration, dateRange, tenant)[planNumber - 1]
}
