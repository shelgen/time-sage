package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.createScheduledEventsForPlan
import com.github.shelgen.timesage.planning.AvailabilityMessage
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanSuggestedScreen(
    val planNumber: Int,
    val dateRange: DateRange,
    val suggestingUserId: Long,
    tenant: Tenant
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val plan = getPlan(planNumber, dateRange, configuration, tenant)
        return if (plan.sessions.isEmpty()) {
            listOf(
                TextDisplay.of(
                    "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests no session this period."
                ),
            )
        } else {
            listOf(
                TextDisplay.of(
                    "## ${DiscordFormatter.mentionUser(suggestingUserId)} suggests this plan:"
                ),
                Container.of(
                    TextDisplay.of(AlternativePrinter(configuration).printAlternative(planNumber, plan))
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
                        val availabilityMessage =
                            PlanningProcessRepository.update(
                                screen.dateRange,
                                screen.tenant
                            ) { availabilitiesPeriod ->
                                availabilitiesPeriod.conclusionMessageId = conclusionMessage.idLong
                                availabilitiesPeriod.availabilityMessage
                            }
                        when (availabilityMessage) {
                            is AvailabilityMessage.Thread ->
                                JDAHolder.jda.getThreadChannelById(availabilityMessage.threadChannelId)
                                    ?.manager?.setArchived(true)?.queue()

                            is AvailabilityMessage.SingleMessage ->
                                rerenderOtherScreen(
                                    messageId = availabilityMessage.messageId,
                                    screen = AvailabilityMessageScreen(screen.dateRange, screen.tenant)
                                )

                            null -> {}
                        }
                        replaceBotPinsWith(conclusionMessage)
                        val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                        val plan = getPlan(screen.planNumber, screen.dateRange, configuration, screen.tenant)
                        if (plan.sessions.isNotEmpty()) {
                            JDAHolder.jda.getGuildById(screen.tenant.server)?.let { guild ->
                                createScheduledEventsForPlan(plan, guild, configuration)
                            }
                        }
                    }
                ) {
                    PlanConcludedWithScreen(
                        planNumber = screen.planNumber,
                        dateRange = screen.dateRange,
                        tenant = screen.tenant
                    )
                }
            }
        }
    }
}
