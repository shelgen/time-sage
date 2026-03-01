package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.createScheduledEventsForPlan
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.AvailabilitiesMonthRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.YearMonth

class PlanAlternativeMonthSuggestedScreen(
    val yearMonth: YearMonth,
    val alternativeNumber: Int,
    val suggestingUserId: Long,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        if (alternativeNumber == 0) {
            return listOf(
                TextDisplay.of(
                    "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests no session this month."
                ),
                ActionRow.of(
                    Buttons.ConcludeWithThisAlternative(screen = this@PlanAlternativeMonthSuggestedScreen).render()
                )
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
            TextDisplay.of(
                "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests this alternative:"
            ),
            Container.of(
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            ),
            ActionRow.of(
                Buttons.ConcludeWithThisAlternative(
                    screen = this@PlanAlternativeMonthSuggestedScreen
                ).render()
            )
        )
    }

    class Buttons {
        class ConcludeWithThisAlternative(override val screen: PlanAlternativeMonthSuggestedScreen) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Conclude with this alternative")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen(
                    onMessagePosted = { conclusionMessage ->
                        AvailabilitiesMonthRepository.update(
                            yearMonth = screen.yearMonth,
                            context = screen.context
                        ) { month ->
                            month.concluded = true
                            month.conclusionMessageId = conclusionMessage.idLong
                        }
                        if (screen.alternativeNumber != 0) {
                            JDAHolder.jda.getGuildById(screen.context.guildId)?.let { guild ->
                                val config = ConfigurationRepository.loadOrInitialize(screen.context)
                                val month = AvailabilitiesMonthRepository.loadOrInitialize(
                                    yearMonth = screen.yearMonth, context = screen.context
                                )
                                val plans = Planner(
                                    configuration = config,
                                    datePeriod = DatePeriod.monthFrom(screen.yearMonth),
                                    responses = month.responses
                                ).generatePossiblePlans()
                                createScheduledEventsForPlan(guild, plans[screen.alternativeNumber - 1], config)
                            }
                        }
                    }
                ) {
                    PlanAlternativeMonthConcludedScreen(
                        yearMonth = screen.yearMonth,
                        alternativeNumber = screen.alternativeNumber,
                        context = screen.context
                    )
                }
            }
        }
    }
}
