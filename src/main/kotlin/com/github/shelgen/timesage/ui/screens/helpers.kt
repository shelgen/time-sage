package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import java.util.*

fun getPlans(dateRange: DateRange, tenant: Tenant): List<Plan> =
    PlanningProcessRepository.load(dateRange, tenant)!!.planAlternatives

fun getPlan(planNumber: Int, dateRange: DateRange, tenant: Tenant): Plan =
    if (planNumber == 0) {
        Plan(id = PlanId(UUID.fromString("00000000-0000-0000-0000-000000000000")), sessions = emptyList())
    } else {
        getPlans(dateRange, tenant)[planNumber - 1]
    }
