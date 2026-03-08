package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.textdisplay.TextDisplay

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
        )
    }

    private fun PlanningProcess.State.toDisplayString() = when (this) {
        PlanningProcess.State.COLLECTING_AVAILABILITIES -> "collecting availabilities"
        PlanningProcess.State.LOCKED -> "locked"
        PlanningProcess.State.CONCLUDED -> "concluded"
    }
}
