package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.createScheduledEventsForPlan
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.planning.Conclusion
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.AlternativePrinter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanSuggestedScreen(
    val planId: PlanId,
    val dateRange: DateRange,
    val suggestingUserId: Long,
    tenant: Tenant
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val plan = getPlan(planId, dateRange, tenant)
        return if (plan.sessions.isEmpty()) {
            listOf(
                TextDisplay.of(
                    "## ${DiscordUserId(suggestingUserId).toMention()} suggests no session this period."
                ),
            )
        } else {
            val displayNumber = getPlanDisplayNumber(planId, dateRange, tenant)
            listOf(
                TextDisplay.of(
                    "## ${DiscordUserId(suggestingUserId).toMention()} suggests this plan:"
                ),
                Container.of(
                    TextDisplay.of(AlternativePrinter(configuration).printAlternative(displayNumber, plan))
                ),
            )
        } + ActionRow.of(
            Buttons.ConcludeWithThisAlternative(this).render()
        )
    }

    class Buttons {
        class ConcludeWithThisAlternative(override val screen: PlanSuggestedScreen) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Conclude with this alternative")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen(
                    onMessagePosted = { conclusionMessage ->
                        val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                        val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                        val plan = getPlan(screen.planId, screen.dateRange, screen.tenant)
                        PlanningProcessRepository.update(planningProcess) { availabilitiesPeriod ->
                            availabilitiesPeriod.conclusion = Conclusion(
                                message = DiscordMessageId(conclusionMessage.idLong),
                                plan = plan.id
                            )
                            availabilitiesPeriod.state = PlanningProcess.State.CONCLUDED
                        }
                        rerenderAvailabilityInterface(planningProcess, configuration)
                        replaceBotPinsWith(conclusionMessage)
                        if (plan.sessions.isNotEmpty()) {
                            JDAHolder.jda.getGuildById(screen.tenant.server.id)?.let { guild ->
                                createScheduledEventsForPlan(plan, guild, configuration)
                            }
                        }
                    }
                ) {
                    PlanConcludedWithScreen(
                        planId = screen.planId,
                        dateRange = screen.dateRange,
                        tenant = screen.tenant
                    )
                }
            }
        }
    }
}
