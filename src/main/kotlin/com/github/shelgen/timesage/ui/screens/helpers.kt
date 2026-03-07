package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository

fun getPlans(
    configuration: Configuration,
    targetPeriod: TargetPeriod,
    tenant: Tenant
): List<Plan> = Planner(
    configuration = configuration,
    targetPeriod = targetPeriod,
    availabilityResponses = AvailabilitiesPeriodRepository.loadOrInitialize(targetPeriod, tenant).availabilityResponses
).generatePossiblePlans()

fun getPlan(
    planNumber: Int,
    targetPeriod: TargetPeriod,
    configuration: Configuration,
    tenant: Tenant
): Plan = if (planNumber == 0) {
    Plan(emptyList())
} else {
    getPlans(configuration, targetPeriod, tenant)[planNumber - 1]
}
