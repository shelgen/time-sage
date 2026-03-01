package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesMonthRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class PlanAlternativeMonthConcludedScreen(
    val yearMonth: YearMonth,
    val alternativeNumber: Int,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val monthHeader = TextDisplay.of(
            "## Plan for ${yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))}:"
        )

        if (alternativeNumber == 0) {
            val allParticipants = configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .sorted()
                .distinct()
            return listOf(monthHeader, TextDisplay.of("No sessions this month.")) +
                    listOfNotNull(
                        allParticipants.takeUnless(List<Long>::isEmpty)?.let { participants ->
                            TextDisplay.of(
                                participants.joinToString(
                                    prefix = DiscordFormatter.bold("Not participating this month") + "\n",
                                    separator = "\n"
                                ) { DiscordFormatter.mentionUser(it) }
                            )
                        }
                    )
        }

        val month = AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = context)
        val planner = Planner(
            configuration = configuration,
            datePeriod = DatePeriod.monthFrom(yearMonth),
            responses = month.responses
        )
        val plans = planner.generatePossiblePlans()
        val plan = plans[alternativeNumber - 1]
        return listOf(
            monthHeader,
            Container.of(
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            )
        ) + listOfNotNull(
            getParticipantsLeftOut(plan, configuration)
                .takeUnless(List<Long>::isEmpty)
                ?.let { participantsLeftOut ->
                    TextDisplay.of(
                        participantsLeftOut.joinToString(
                            prefix = DiscordFormatter.bold("Not participating this month") + "\n",
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
