package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.atNormalStartTime
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.getCampaign
import com.github.shelgen.timesage.repositories.getParticipants

class AlternativePrinter(private val configuration: ConfigurationRepository.ConfigurationDto) {
    fun printAlternative(alternativeNumber: Int, plan: Planner.Plan) =
        "-# Alternative $alternativeNumber (Score ${plan.score.toShortString()})\n" +
                plan.print()

    private fun Planner.Plan.print() =
        sessions.joinToString(separator = "\n") { "### ${it.print()}" }

    private fun Planner.Plan.Session.print() =
        "${printCampaign()} on ${printTime()}\n" +
                printPlayers()

    private fun Planner.Plan.Session.printTime() =
        DiscordFormatter.timestamp(date.atNormalStartTime(), DiscordFormatter.TimestampFormat.LONG_DATE_TIME)

    private fun Planner.Plan.Session.printCampaign(): String =
        DiscordFormatter.bold(configuration.getCampaign(campaignId).name)

    private fun Planner.Plan.Session.printPlayers() =
        configuration.getCampaign(campaignId).getParticipants()
            .joinToString(
                separator = "\n",
                postfix = "\n"
            ) { playerDiscordId ->
                attendees.firstOrNull { it.playerDiscordId == playerDiscordId }
                    ?.let { (_, ifNeedBe) ->
                        if (ifNeedBe) {
                            DiscordFormatter.italics(DiscordFormatter.mentionUser(playerDiscordId) + " (if need be)")
                        } else {
                            DiscordFormatter.mentionUser(playerDiscordId)
                        }
                    }
                    ?: (DiscordFormatter.strikethrough(DiscordFormatter.mentionUser(playerDiscordId)) + " (unavailable)")
            }
}
