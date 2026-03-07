package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.textdisplay.TextDisplay

class AvailabilityThreadStartScreen(val dateRange: DateRange, tenant: Tenant) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<TextDisplay> {
        val dateRangeState = AvailabilitiesPeriodRepository.loadOrInitialize(dateRange, tenant)
        return listOf(
            TextDisplay.of(
                "## Availabilities for ${dateRange.toLocalizedString(configuration.localization)}\n" +
                        if (dateRangeState.concluded) {
                            DiscordFormatter.bold(
                                "✅ Planning for this ${dateRange.toLocalizedString(configuration.localization)} has been concluded" +
                                        dateRangeState.conclusionMessageId?.let {
                                            "\nSee https://discord.com/channels/${tenant.guildId}/${tenant.channelId}/$it"
                                        }.orEmpty()
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
