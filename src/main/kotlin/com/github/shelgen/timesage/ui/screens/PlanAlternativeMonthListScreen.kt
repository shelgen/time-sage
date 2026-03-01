package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesMonthRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.YearMonth

class PlanAlternativeMonthListScreen(
    val yearMonth: YearMonth,
    val startIndex: Int,
    val size: Int,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val month = AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = context)
        val planner = Planner(
            configuration = configuration,
            datePeriod = DatePeriod.monthFrom(yearMonth),
            responses = month.responses
        )
        val plans = planner.generatePossiblePlans()
        val alternativeNumberedPlans =
            plans.drop(startIndex).take(size)
                .mapIndexed { index, plan -> startIndex + index + 1 to plan }
        val totalNumAlternatives = plans.size

        return if (alternativeNumberedPlans.isEmpty()) {
            listOf(
                TextDisplay.of("There are no possible alternatives given the current availabilities."),
                ActionRow.of(Buttons.SuggestNoSession(screen = this@PlanAlternativeMonthListScreen).render())
            )
        } else {
            val nextStartIndex = startIndex + size
            val nextSize = size.coerceAtMost(totalNumAlternatives - nextStartIndex)
            listOf(
                renderHeader(alternativeNumberedPlans, totalNumAlternatives),
                renderAlternatives(alternativeNumberedPlans, configuration),
                renderBottomActionRow(nextSize)
            ).flatten()
        }
    }

    private fun renderHeader(alternativeNumberedPlans: List<Pair<Int, Plan>>, totalNumAlternatives: Int) =
        listOf(
            TextDisplay.of(
                "Alternatives " +
                        alternativeNumberedPlans.first().component1().toString() +
                        " through " +
                        alternativeNumberedPlans.last().component1().toString() +
                        " out of $totalNumAlternatives:"
            )
        )

    private fun renderAlternatives(
        alternativeNumberedPlans: List<Pair<Int, Plan>>,
        configuration: Configuration
    ) =
        alternativeNumberedPlans.map { (alternativeNumber, plan) ->
            renderAlternative(alternativeNumber, plan, configuration)
        }

    private fun renderAlternative(alternativeNumber: Int, plan: Plan, configuration: Configuration) =
        Container.of(
            Section.of(
                Buttons.SuggestAlternative(
                    alternativeNumber = alternativeNumber,
                    alternativeHashcode = plan.hashCode(),
                    screen = this@PlanAlternativeMonthListScreen
                ).render(),
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            )
        )

    private fun renderBottomActionRow(nextSize: Int): List<MessageTopLevelComponent> {
        val buttons = buildList {
            if (nextSize > 0) add(Buttons.ShowMoreAlternatives(nextSize = nextSize, screen = this@PlanAlternativeMonthListScreen).render())
            if (startIndex == 0) add(Buttons.SuggestNoSession(screen = this@PlanAlternativeMonthListScreen).render())
        }
        return if (buttons.isEmpty()) emptyList() else listOf(ActionRow.of(buttons))
    }

    class Buttons {
        class SuggestAlternative(
            val alternativeNumber: Int,
            val alternativeHashcode: Int,
            override val screen: PlanAlternativeMonthListScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Suggest this")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanAlternativeMonthSuggestedScreen(
                        yearMonth = screen.yearMonth,
                        alternativeNumber = alternativeNumber,
                        suggestingUserId = event.user.idLong,
                        context = screen.context
                    )
                }
            }
        }

        class ShowMoreAlternatives(
            val nextSize: Int,
            override val screen: PlanAlternativeMonthListScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Show $nextSize more...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddEphemeralScreen {
                    PlanAlternativeMonthListScreen(
                        yearMonth = screen.yearMonth,
                        startIndex = screen.startIndex + screen.size,
                        size = nextSize,
                        context = screen.context
                    )
                }
            }
        }

        class SuggestNoSession(override val screen: PlanAlternativeMonthListScreen) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "No session")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanAlternativeMonthSuggestedScreen(
                        yearMonth = screen.yearMonth,
                        alternativeNumber = 0,
                        suggestingUserId = event.user.idLong,
                        context = screen.context
                    )
                }
            }
        }
    }
}
