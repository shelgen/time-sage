package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.configuration.Configuration
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
            .map(ActivityMember::userId)
            .sorted()
            .joinToString(
                separator = "\n",
                postfix = "\n"
            ) { userId ->
                participation.firstOrNull { it.userId == userId }
                    ?.let { (_, ifNeedBe) ->
                        if (ifNeedBe) {
                            DiscordFormatter.italics(DiscordFormatter.mentionUser(userId) + " (if need be)")
                        } else {
                            DiscordFormatter.mentionUser(userId)
                        }
                    }
                    ?: (DiscordFormatter.strikethrough(DiscordFormatter.mentionUser(userId)) + " (unavailable)")
            }
}
