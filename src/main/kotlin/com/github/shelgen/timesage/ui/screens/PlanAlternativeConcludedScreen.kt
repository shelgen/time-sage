package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.weekDatesStartingWith
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.LocalDate

class PlanAlternativeConcludedScreen(
    val weekStartDate: LocalDate,
    val alternativeNumber: Int,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val dates = weekDatesStartingWith(weekStartDate)
        val weekHeader = TextDisplay.of(
            "## Plan for the week of " +
                    dates.first().formatAsShortDate() +
                    " through " +
                    dates.last().formatAsShortDate() +
                    ":"
        )

        if (alternativeNumber == 0) {
            val allParticipants = configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .sorted()
                .distinct()
            return listOf(weekHeader, TextDisplay.of("No sessions this week.")) +
                    listOfNotNull(
                        allParticipants.takeUnless(List<Long>::isEmpty)?.let { participants ->
                            TextDisplay.of(
                                participants.joinToString(
                                    prefix = DiscordFormatter.bold("Not participating this week") + "\n",
                                    separator = "\n"
                                ) { DiscordFormatter.mentionUser(it) }
                            )
                        }
                    )
        }

        val week = AvailabilitiesWeekRepository.loadOrInitialize(startDate = weekStartDate, context = context)
        val planner = Planner(configuration = configuration, weekStartDate = weekStartDate, week = week)
        val plans = planner.generatePossiblePlans()
        val plan = plans[alternativeNumber - 1]
        return listOf(
            weekHeader,
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

    private fun getParticipantsLeftOut(plan: Plan, configuration: Configuration) =
        configuration.activities
            .flatMap(Activity::participants)
            .map(Participant::userId)
            .minus(
                plan.sessions
                    .flatMap(Plan.Session::attendees)
                    .map(Plan.Session.Attendee::userId)
                    .toSet()
            )
            .sorted()
            .distinct()
}
