package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
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
    val weekStartDate: LocalDate,
    val alternativeNumber: Int,
    val suggestingUserId: Long,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        if (alternativeNumber == 0) {
            return listOf(
                TextDisplay.of(
                    "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests no session this week."
                ),
                ActionRow.of(
                    Buttons.ConcludeWithThisAlternative(screen = this@PlanAlternativeSuggestedScreen).render()
                )
            )
        }
        val week = AvailabilitiesWeekRepository.loadOrInitialize(startDate = weekStartDate, context = context)
        val planner = Planner(configuration = configuration, weekStartDate = weekStartDate, week = week)
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

    class Buttons {
        class ConcludeWithThisAlternative(override val screen: PlanAlternativeSuggestedScreen) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Conclude with this alternative")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen(
                    onMessagePosted = { conclusionMessage ->
                        val availabilityMessageId = AvailabilitiesWeekRepository.update(
                            startDate = screen.weekStartDate,
                            context = screen.context
                        ) { week ->
                            week.concluded = true
                            week.conclusionMessageId = conclusionMessage.idLong
                            week.messageId
                        }
                        if (availabilityMessageId != null) {
                            rerenderOtherScreen(
                                messageId = availabilityMessageId,
                                screen = AvailabilityScreen(
                                    startDate = screen.weekStartDate,
                                    context = screen.context
                                )
                            )
                        }
                        replaceBotPinsWith(conclusionMessage)
                    }
                ) {
                    PlanAlternativeConcludedScreen(
                        weekStartDate = screen.weekStartDate,
                        alternativeNumber = screen.alternativeNumber,
                        context = screen.context
                    )
                }
            }
        }
    }
}
