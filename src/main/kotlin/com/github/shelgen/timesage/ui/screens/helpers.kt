package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.planning.PlanningProcess
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.util.*

val NO_SESSION_PLAN_ID = PlanId(UUID.fromString("00000000-0000-0000-0000-000000000000"))

fun planningProcessNotFound(): List<MessageTopLevelComponent> = listOf(
    Container.of(
        TextDisplay.of(
            "The planning process in question does not exist. It may have been deleted. " +
                    "Please try whatever you were doing again."
        )
    ).withAccentColor(0xE57373)
)

fun getPlan(planId: PlanId, planningProcess: PlanningProcess): Plan =
    if (planId == NO_SESSION_PLAN_ID) {
        Plan(id = NO_SESSION_PLAN_ID, sessions = emptyList())
    } else {
        planningProcess.planAlternatives.first { it.id == planId }
    }

fun getPlanDisplayNumber(planId: PlanId, planningProcess: PlanningProcess): Int =
    planningProcess.planAlternatives.indexOfFirst { it.id == planId } + 1
