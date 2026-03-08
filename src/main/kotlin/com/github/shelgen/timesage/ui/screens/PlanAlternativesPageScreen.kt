package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.ui.AlternativePrinter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanAlternativesPageScreen(
    val dateRange: DateRange,
    val fromInclusive: Int,
    val pageSize: Int,
    tenant: Tenant
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val allPlans = getPlans(configuration, dateRange, tenant)
        val numberedPlansInPage =
            allPlans.drop(fromInclusive).take(pageSize)
                .mapIndexed { index, plan -> fromInclusive + index + 1 to plan }
        val header = if (allPlans.isEmpty()) {
            listOf(
                TextDisplay.of("There are no possible alternatives given the current availabilities."),
                ActionRow.of(Buttons.SuggestNoSession(screen = this@PlanAlternativesPageScreen).render())
            )
        } else if (numberedPlansInPage.isEmpty()) {
            listOf(TextDisplay.of("There are no more plans (only ${allPlans.size} in total)."))
        } else {
            renderHeader(numberedPlansInPage, allPlans.size)
        }
        return listOf(
            header,
            renderAlternatives(numberedPlansInPage, configuration),
            renderBottomActionRow(pageSize.coerceAtMost(allPlans.size - (fromInclusive + pageSize)))
        ).flatten()

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
                    planNumber = alternativeNumber,
                    alternativeHashcode = plan.hashCode(),
                    screen = this@PlanAlternativesPageScreen
                ).render(),
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            )
        )

    private fun renderBottomActionRow(nextSize: Int): List<MessageTopLevelComponent> {
        val buttons = buildList {
            if (nextSize > 0) add(
                Buttons.ShowMoreAlternatives(
                    nextSize = nextSize,
                    screen = this@PlanAlternativesPageScreen
                ).render()
            )
            if (fromInclusive == 0) add(Buttons.SuggestNoSession(screen = this@PlanAlternativesPageScreen).render())
        }
        return if (buttons.isEmpty()) emptyList() else listOf(ActionRow.of(buttons))
    }

    class Buttons {
        class SuggestAlternative(
            val planNumber: Int,
            val alternativeHashcode: Int,
            override val screen: PlanAlternativesPageScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Suggest this")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanSuggestedScreen(
                        planNumber = planNumber,
                        dateRange = screen.dateRange,
                        suggestingUserId = event.user.idLong,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class ShowMoreAlternatives(
            val nextSize: Int,
            override val screen: PlanAlternativesPageScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Show $nextSize more...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddEphemeralScreen {
                    PlanAlternativesPageScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = screen.fromInclusive + screen.pageSize,
                        pageSize = nextSize,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class SuggestNoSession(override val screen: PlanAlternativesPageScreen) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "No session")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanSuggestedScreen(
                        planNumber = 0,
                        dateRange = screen.dateRange,
                        suggestingUserId = event.user.idLong,
                        tenant = screen.tenant
                    )
                }
            }
        }
    }
}
