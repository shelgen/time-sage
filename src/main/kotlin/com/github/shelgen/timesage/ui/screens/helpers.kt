package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import java.util.*

fun getPlans(
    configuration: Configuration,
    dateRange: DateRange,
    tenant: Tenant
): List<Plan> = Planner(
    configuration = configuration,
    dateRange = dateRange,
    planningProcess = PlanningProcessRepository.load(dateRange, tenant)!!
).generatePossiblePlans()

fun getPlan(
    planNumber: Int,
    dateRange: DateRange,
    configuration: Configuration,
    tenant: Tenant
): Plan = if (planNumber == 0) {
    Plan(id = PlanId(UUID.fromString("00000000-0000-0000-0000-000000000000")), sessions = emptyList())
} else {
    getPlans(configuration, dateRange, tenant)[planNumber - 1]
}
