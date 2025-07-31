package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.LocalDate

class PlanAlternativeSuggestedScreen(
    val weekMondayDate: LocalDate,
    val alternativeNumber: Int,
    val suggestingUserId: Long,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val week = WeekRepository.loadOrInitialize(mondayDate = weekMondayDate, context = context)
        val planner = Planner(configuration = configuration, week = week)
        val plans = planner.generatePossiblePlans()
        val plan = plans[alternativeNumber - 1]
        return listOf(
            TextDisplay.of(
                "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests this alternative:"
            ),
            Container.of(
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            ),
            ActionRow.of(
                Buttons.ConcludeWithThisAlternative(
                    screen = this@PlanAlternativeSuggestedScreen
                ).render()
            )
        )
    }

    override fun parameters(): List<String> =
        listOf(
            weekMondayDate.toString(),
            alternativeNumber.toString(),
            suggestingUserId.toString()
        )

    companion object {
        fun reconstruct(parameters: List<String>, context: OperationContext) = PlanAlternativeSuggestedScreen(
            weekMondayDate = LocalDate.parse(parameters[0]),
            alternativeNumber = parameters[1].toInt(),
            suggestingUserId = parameters[2].toLong(),
            context = context
        )
    }

    class Buttons {
        class ConcludeWithThisAlternative(screen: PlanAlternativeSuggestedScreen) :
            ScreenButton<PlanAlternativeSuggestedScreen>(
                screen = screen
            ) {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Conclude with this alternative")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanAlternativeConcludedScreen(
                        weekMondayDate = screen.weekMondayDate,
                        alternativeNumber = screen.alternativeNumber,
                        context = screen.context
                    )
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<PlanAlternativeSuggestedScreen, ConcludeWithThisAlternative>(
                    screenClass = PlanAlternativeSuggestedScreen::class,
                    componentClass = ConcludeWithThisAlternative::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    context: OperationContext
                ) =
                    ConcludeWithThisAlternative(screen = reconstruct(parameters = screenParameters, context = context))
            }
        }
    }
}
