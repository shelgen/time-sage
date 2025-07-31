package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.weekDatesForMonday
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.LocalDate

class PlanAlternativeConcludedScreen(
    val weekMondayDate: LocalDate,
    val alternativeNumber: Int,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val week = WeekRepository.loadOrInitialize(mondayDate = weekMondayDate, context = context)
        val planner = Planner(configuration = configuration, week = week)
        val plans = planner.generatePossiblePlans()
        val plan = plans[alternativeNumber - 1]
        val dates = weekDatesForMonday(weekMondayDate)
        return listOf(
            TextDisplay.of(
                "## Plan for the week of " +
                        dates.first().formatAsShortDate() +
                        " through " +
                        dates.last().formatAsShortDate() +
                        ":"
            ),
            Container.of(
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            )
        ) + listOfNotNull(
            getParticipantsLeftOut(plan, configuration)
                .takeUnless(List<Long>::isEmpty)
                ?.let { participantsLeftOut ->
                    TextDisplay.of(
                        participantsLeftOut.joinToString(
                            prefix = DiscordFormatter.bold("Not participating this week") + "\n",
                            separator = "\n"
                        ) {
                            DiscordFormatter.mentionUser(it)
                        }
                    )
                }
        )
    }

    override fun parameters(): List<String> =
        listOf(
            weekMondayDate.toString(),
            alternativeNumber.toString(),
        )

    companion object {
        fun reconstruct(parameters: List<String>, context: OperationContext) = PlanAlternativeConcludedScreen(
            weekMondayDate = LocalDate.parse(parameters[0]),
            alternativeNumber = parameters[1].toInt(),
            context = context
        )
    }

    private fun getParticipantsLeftOut(plan: Planner.Plan, configuration: Configuration) =
        configuration.activities
            .flatMap(Activity::participants)
            .map(Participant::userId)
            .minus(
                plan.sessions
                    .flatMap(Planner.Plan.Session::attendees)
                    .map(Planner.Plan.Session.Attendee::userId)
                    .toSet()
            )
            .sorted()
}
