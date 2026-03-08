package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.AlternativePrinter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PreviewAlternativesScreen(
    val dateRange: DateRange,
    val fromInclusive: Int,
    val pageSize: Int,
    val inputHashCode: Int,
    tenant: Tenant,
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, tenant)
            ?: return listOf(TextDisplay.of("Error: planning process not found."))

        val planner = Planner(configuration, planningProcess)

        if (planner.hashCodeForInput() != inputHashCode) {
            return listOf(
                TextDisplay.of(
                    "⚠️ This preview is outdated because the availability has changed. " +
                            "Click **Preview alternatives** again to see an updated preview."
                )
            )
        }

        val allPlans = planner.generatePossiblePlans()
        val numberedPlansInPage = allPlans.drop(fromInclusive).take(pageSize)
            .mapIndexed { index, plan -> fromInclusive + index + 1 to plan }

        val header: List<MessageTopLevelComponent> = when {
            allPlans.isEmpty() ->
                listOf(TextDisplay.of("There are no possible alternatives given the current availabilities."))
            numberedPlansInPage.isEmpty() ->
                listOf(TextDisplay.of("There are no more plans (only ${allPlans.size} in total)."))
            else ->
                listOf(
                    TextDisplay.of(
                        "Alternatives ${numberedPlansInPage.first().first} " +
                                "through ${numberedPlansInPage.last().first} " +
                                "out of ${allPlans.size}:"
                    )
                )
        }

        val alternatives = numberedPlansInPage.map { (number, plan) ->
            Container.of(
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(number, plan))
            )
        }

        val nextSize = pageSize.coerceAtMost(allPlans.size - (fromInclusive + pageSize))
        val bottomRow: List<MessageTopLevelComponent> = if (nextSize > 0) {
            listOf(ActionRow.of(Buttons.ShowMoreAlternatives(nextSize = nextSize, screen = this).render()))
        } else {
            emptyList()
        }

        return header + alternatives + bottomRow
    }

    class Buttons {
        class ShowMoreAlternatives(
            val nextSize: Int,
            override val screen: PreviewAlternativesScreen,
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Show $nextSize more...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PreviewAlternativesScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = screen.fromInclusive + screen.pageSize,
                        pageSize = nextSize,
                        inputHashCode = screen.inputHashCode,
                        tenant = screen.tenant,
                    )
                }
            }
        }
    }
}
