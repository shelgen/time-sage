package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.*
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.PlannedSession
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay

class PlanConcludedWithScreen(
    val planNumber: Int,
    val targetPeriod: TargetPeriod,
    tenant: Tenant
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val plan = getPlan(planNumber, targetPeriod, configuration, tenant)
        return listOf(TextDisplay.of("## Plan for ${targetPeriod.toLocalizedString(configuration.localization)}:")) +
                if (plan.sessions.isEmpty()) {
                    TextDisplay.of("No sessions this period.")
                } else {
                    Container.of(
                        TextDisplay.of(AlternativePrinter(configuration).printAlternative(planNumber, plan))
                    )
                } + listOfNotNull(
            getParticipantsLeftOut(plan, configuration)
                .takeUnless(List<Long>::isEmpty)
                ?.let { participantsLeftOut ->
                    TextDisplay.of(
                        participantsLeftOut.joinToString(
                            prefix = DiscordFormatter.bold("Not participating this period") + "\n",
                            separator = "\n"
                        ) {
                            DiscordFormatter.mentionUser(it)
                        }
                    )
                }
        )
    }

    private fun getParticipantsLeftOut(plan: Plan, configuration: Configuration) =
        configuration.activities
            .asSequence()
            .flatMap(Activity::members)
            .map(ActivityMember::userId)
            .minus(
                plan.sessions
                    .flatMap(PlannedSession::participants)
                    .map(PlannedSession.Participant::userId)
                    .toSet()
            )
            .sorted()
            .distinct()
            .toList()
}
