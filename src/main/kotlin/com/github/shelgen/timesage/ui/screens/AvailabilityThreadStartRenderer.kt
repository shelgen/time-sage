package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.textdisplay.TextDisplay

object AvailabilityThreadStartRenderer {
    fun renderComponents(dateRange: DateRange, configuration: Configuration): List<TextDisplay> {
        val tenant = configuration.tenant
        val planningProcess = PlanningProcessRepository.load(dateRange, tenant)
            ?: error("Planning process for $dateRange in $tenant does not exist!")
        val conclusion = planningProcess.conclusion
        return listOf(
            TextDisplay.of(
                "## Availabilities for ${dateRange.toLocalizedString(configuration.localization)}\n" +
                        if (conclusion != null) {
                            DiscordFormatter.bold(
                                "✅ Planning for this ${dateRange.toLocalizedString(configuration.localization)} has been concluded" +
                                        "\nSee https://discord.com/channels/${tenant.server}/${tenant.textChannel}/${conclusion.message.id}"
                            )
                        } else {
                            "Please use the buttons below to toggle your availability" +
                                    formatActivities(configuration.activities)
                        }
            )
        )
    }

    private fun formatActivities(activities: List<Activity>) =
        when (activities.size) {
            0 -> ""
            1 -> " for\n${DiscordFormatter.bold(activities.first().name)}"
            else -> " for one or more of\n" +
                    activities.map(Activity::name)
                        .sorted()
                        .map(DiscordFormatter::bold)
                        .joinToString("\n") { "- $it" }
        }
}
