package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.Session

class AlternativePrinter(private val configuration: Configuration) {
    fun printAlternative(alternativeNumber: Int, plan: Plan) =
        "-# Alternative $alternativeNumber (${plan.score.toDisplayString()})\n" +
                plan.print()

    private fun Plan.print() =
        sessions.joinToString(separator = "\n") { "### ${it.print()}" }

    private fun Session.print() =
        "${printActivity()} on ${printTime()}\n" +
                printParticipants()

    private fun Session.printTime() =
        DiscordFormatter.timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)

    private fun Session.printActivity(): String =
        DiscordFormatter.bold(configuration.getActivity(activityId).name)

    private fun Session.printParticipants() =
        configuration.getActivity(activityId).members
            .map(Member::user)
            .sortedBy(DiscordUserId::id)
            .joinToString(
                separator = "\n",
                postfix = "\n"
            ) { user ->
                val participant = participants.firstOrNull { it.user == user }
                if (participant != null) {
                    if (participant.ifNeedBe) {
                        DiscordFormatter.italics(user.toMention() + " (if need be)")
                    } else {
                        user.toMention()
                    }
                } else {
                    DiscordFormatter.strikethrough(user.toMention()) + " (unavailable)"
                }
            }
}
