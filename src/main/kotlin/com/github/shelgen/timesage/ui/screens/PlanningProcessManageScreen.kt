package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanningProcessManageScreen(
    val dateRange: DateRange,
    tenant: Tenant,
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val process = PlanningProcessRepository.load(dateRange, tenant)
            ?: return listOf(TextDisplay.of("Error: Planning process not found."))

        val availabilityLink = process.availabilityInterface.toLink(tenant)

        return listOf(
            TextDisplay.of("## Planning: ${dateRange.toLocalizedString(configuration.localization)}"),
            TextDisplay.of(availabilityLink),
            TextDisplay.of("Currently ${DiscordFormatter.bold(process.state.toDisplayString())}"),
            TextDisplay.of(process.conclusion?.let {
                "Conclusion: https://discord.com/channels/${tenant.server}/${tenant.textChannel}/${it.message.id}"
            } ?: "No conclusion yet."),
            TextDisplay.of(process.sentReminders.lastOrNull()?.let {
                "Last reminder sent: ${configuration.localization.dateOf(it.sentAt)}"
            } ?: "No reminders sent yet."),
        ) + when (process.state) {
            PlanningProcess.State.COLLECTING_AVAILABILITIES ->
                listOf(ActionRow.of(Buttons.LockAndPickPlan(this).render()))
            PlanningProcess.State.LOCKED ->
                listOf(ActionRow.of(Buttons.PickPlans(this).render()))
            PlanningProcess.State.CONCLUDED -> emptyList()
        }
    }

    private fun PlanningProcess.State.toDisplayString() = when (this) {
        PlanningProcess.State.COLLECTING_AVAILABILITIES -> "collecting availabilities"
        PlanningProcess.State.LOCKED -> "locked"
        PlanningProcess.State.CONCLUDED -> "concluded"
    }

    class Buttons {
        class LockAndPickPlan(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.primary(CustomIdSerialization.serialize(this), "Lock and pick plan")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    val tenant = screen.tenant
                    val dateRange = screen.dateRange
                    val configuration = ConfigurationRepository.loadOrInitialize(tenant)
                    val planningProcess = PlanningProcessRepository.load(dateRange, tenant)!!
                    val plans = Planner(configuration, planningProcess).generatePossiblePlans()
                    PlanningProcessRepository.update(planningProcess) { planningProcess ->
                        planningProcess.state = PlanningProcess.State.LOCKED
                        planningProcess.planAlternatives = plans
                    }
                    rerenderAvailabilityInterface(planningProcess, configuration)
                    PlanAlternativesPageScreen(
                        dateRange = dateRange,
                        fromInclusive = 0,
                        pageSize = 3,
                        tenant = tenant
                    )
                }
            }
        }

        class PickPlans(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.primary(CustomIdSerialization.serialize(this), "Pick plans...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanAlternativesPageScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = 0,
                        pageSize = 3,
                        tenant = screen.tenant
                    )
                }
            }
        }
    }
}
