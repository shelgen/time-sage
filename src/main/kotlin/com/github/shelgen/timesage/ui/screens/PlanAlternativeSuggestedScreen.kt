package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.createScheduledEventsForPlan
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.LocalDate
import java.time.YearMonth

class PlanAlternativeSuggestedScreen(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val alternativeNumber: Int,
    val suggestingUserId: Long,
    context: OperationContext
) : Screen(context) {

    private fun periodWord(): String {
        val ym = YearMonth.from(periodStart)
        return when {
            ym.atDay(1) == periodStart && ym.atEndOfMonth() == periodEnd -> "month"
            periodEnd == periodStart.plusDays(6) -> "week"
            else -> "period"
        }
    }

    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        if (alternativeNumber == 0) {
            return listOf(
                TextDisplay.of(
                    "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests no session this ${periodWord()}."
                ),
                ActionRow.of(
                    Buttons.ConcludeWithThisAlternative(screen = this@PlanAlternativeSuggestedScreen).render()
                )
            )
        }
        val period = DatePeriod(periodStart, periodEnd)
        val data = AvailabilitiesPeriodRepository.loadOrInitialize(period, context)
        val planner = Planner(configuration = configuration, datePeriod = period, responses = data.responses)
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
                        val period = DatePeriod(screen.periodStart, screen.periodEnd)
                        val (messageId, threadId) = AvailabilitiesPeriodRepository.update(
                            period = period,
                            context = screen.context
                        ) { p ->
                            p.concluded = true
                            p.conclusionMessageId = conclusionMessage.idLong
                            p.messageId to p.threadId
                        }
                        if (threadId != null) {
                            JDAHolder.jda.getThreadChannelById(threadId)?.manager?.setArchived(true)?.queue()
                        } else if (messageId != null) {
                            rerenderOtherScreen(
                                messageId = messageId,
                                screen = AvailabilityScreen(
                                    periodStart = screen.periodStart,
                                    periodEnd = screen.periodEnd,
                                    pageIndex = AvailabilityScreen.SINGLE_PAGE,
                                    mainChannelId = screen.context.channelId,
                                    context = screen.context
                                )
                            )
                        }
                        replaceBotPinsWith(conclusionMessage)
                        if (screen.alternativeNumber != 0) {
                            JDAHolder.jda.getGuildById(screen.context.guildId)?.let { guild ->
                                val config = ConfigurationRepository.loadOrInitialize(screen.context)
                                val data = AvailabilitiesPeriodRepository.loadOrInitialize(period, screen.context)
                                val plans = Planner(
                                    configuration = config,
                                    datePeriod = period,
                                    responses = data.responses
                                ).generatePossiblePlans()
                                createScheduledEventsForPlan(guild, plans[screen.alternativeNumber - 1], config)
                            }
                        }
                    }
                ) {
                    PlanAlternativeConcludedScreen(
                        periodStart = screen.periodStart,
                        periodEnd = screen.periodEnd,
                        alternativeNumber = screen.alternativeNumber,
                        context = screen.context
                    )
                }
            }
        }
    }
}
