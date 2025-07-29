package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.LocalDate

class PlanAlternativeSuggestedScreen(
    val weekMondayDate: LocalDate,
    val alternativeNumber: Int,
    val suggestingUserId: Long,
    guildId: Long
) : Screen(guildId) {
    override fun renderComponents(): List<MessageTopLevelComponent> {
        val plan = Planner(
            configuration = configuration,
            week = WeekRepository.load(
                guildId = guildId,
                weekMondayDate = weekMondayDate
            )
        ).generatePossiblePlans()[alternativeNumber - 1]
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
        fun reconstruct(parameters: List<String>, guildId: Long) = PlanAlternativeSuggestedScreen(
            weekMondayDate = LocalDate.parse(parameters[0]),
            alternativeNumber = parameters[1].toInt(),
            suggestingUserId = parameters[2].toLong(),
            guildId = guildId
        )
    }

    class Buttons {
        class ConcludeWithThisAlternative(screen: PlanAlternativeSuggestedScreen) :
            ScreenButton<PlanAlternativeSuggestedScreen>(
                style = ButtonStyle.SUCCESS,
                label = "Conclude with this alternative",
                screen = screen
            ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanAlternativeConcludedScreen(
                        weekMondayDate = screen.weekMondayDate,
                        alternativeNumber = screen.alternativeNumber,
                        guildId = screen.guildId
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
                    guildId: Long
                ) =
                    ConcludeWithThisAlternative(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }
    }
}
