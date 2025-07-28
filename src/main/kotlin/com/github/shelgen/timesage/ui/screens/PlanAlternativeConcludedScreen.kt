package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.ConfigurationRepository
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
) : Screen() {
    override fun renderComponents(): List<MessageTopLevelComponent> {
        val plan = Planner(
            configuration = ConfigurationRepository.load(),
            week = WeekRepository.load(weekMondayDate)
        ).generatePossiblePlans()[alternativeNumber - 1]
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
                TextDisplay.of(AlternativePrinter.printAlternative(alternativeNumber, plan))
            )
        ) + listOfNotNull(
            getNonParticipatingPlayers(plan)
                .takeUnless(List<Long>::isEmpty)
                ?.let { nonParticipatingPlayers ->
                    TextDisplay.of(
                        nonParticipatingPlayers.joinToString(
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
        fun reconstruct(parameters: List<String>) = PlanAlternativeConcludedScreen(
            weekMondayDate = LocalDate.parse(parameters[0]),
            alternativeNumber = parameters[1].toInt(),
        )
    }

    private fun getNonParticipatingPlayers(plan: Planner.Plan) =
        ConfigurationRepository.load()
            .campaigns
            .flatMap { it.gmDiscordIds + it.playerDiscordIds }
            .distinct()
            .minus(
                plan.sessions
                    .flatMap(Planner.Plan.Session::attendees)
                    .map(Planner.Plan.Session.Attendee::playerDiscordId)
                    .toSet()
            )
            .sorted()
}
