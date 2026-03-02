package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.formatAsShortDate
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class PlanAlternativeConcludedScreen(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val alternativeNumber: Int,
    context: OperationContext
) : Screen(context) {

    private fun correspondingYearMonth(): YearMonth? {
        val ym = YearMonth.from(periodStart)
        return if (ym.atDay(1) == periodStart && ym.atEndOfMonth() == periodEnd) ym else null
    }

    private fun periodWord(): String {
        return when {
            correspondingYearMonth() != null -> "month"
            periodEnd == periodStart.plusDays(6) -> "week"
            else -> "period"
        }
    }

    private fun periodLabel(): String {
        val ym = correspondingYearMonth()
        return if (ym != null) {
            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        } else {
            "${periodStart.formatAsShortDate()} through ${periodEnd.formatAsShortDate()}"
        }
    }

    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val header = TextDisplay.of("## Plan for ${periodLabel()}:")

        if (alternativeNumber == 0) {
            val allParticipants = configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .sorted()
                .distinct()
            return listOf(header, TextDisplay.of("No sessions this ${periodWord()}.")) +
                    listOfNotNull(
                        allParticipants.takeUnless(List<Long>::isEmpty)?.let { participants ->
                            TextDisplay.of(
                                participants.joinToString(
                                    prefix = DiscordFormatter.bold("Not participating this ${periodWord()}") + "\n",
                                    separator = "\n"
                                ) { DiscordFormatter.mentionUser(it) }
                            )
                        }
                    )
        }

        val period = DatePeriod(periodStart, periodEnd)
        val data = AvailabilitiesPeriodRepository.loadOrInitialize(period, context)
        val planner = Planner(configuration = configuration, datePeriod = period, responses = data.responses)
        val plans = planner.generatePossiblePlans()
        val plan = plans[alternativeNumber - 1]
        return listOf(
            header,
            Container.of(
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            )
        ) + listOfNotNull(
            getParticipantsLeftOut(plan, configuration)
                .takeUnless(List<Long>::isEmpty)
                ?.let { participantsLeftOut ->
                    TextDisplay.of(
                        participantsLeftOut.joinToString(
                            prefix = DiscordFormatter.bold("Not participating this ${periodWord()}") + "\n",
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
