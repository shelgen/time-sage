package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.Session
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay

class PlanConcludedWithScreen(
    val planNumber: Int,
    val dateRange: DateRange,
    tenant: Tenant
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val plan = getPlan(planNumber, dateRange, configuration, tenant)
        return listOf(TextDisplay.of("## Plan for ${dateRange.toLocalizedString(configuration.localization)}:")) +
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
                    .flatMap(Session::participation)
                    .map(Session.Participant::userId)
                    .toSet()
            )
            .sorted()
            .distinct()
            .toList()
}
