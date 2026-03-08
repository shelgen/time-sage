package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import java.util.*

val NO_SESSION_PLAN_ID = PlanId(UUID.fromString("00000000-0000-0000-0000-000000000000"))

fun getPlans(dateRange: DateRange, tenant: Tenant): List<Plan> =
    PlanningProcessRepository.load(dateRange, tenant)!!.planAlternatives

fun getPlan(planId: PlanId, dateRange: DateRange, tenant: Tenant): Plan =
    if (planId == NO_SESSION_PLAN_ID) {
        Plan(id = NO_SESSION_PLAN_ID, sessions = emptyList())
    } else {
        getPlans(dateRange, tenant).first { it.id == planId }
    }

fun getPlanDisplayNumber(planId: PlanId, dateRange: DateRange, tenant: Tenant): Int =
    getPlans(dateRange, tenant).indexOfFirst { it.id == planId } + 1
