package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.atNormalStartTime
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.planning.Planner

class AlternativePrinter(private val configuration: Configuration) {
    fun printAlternative(alternativeNumber: Int, plan: Planner.Plan) =
        "-# Alternative $alternativeNumber (Score ${plan.score.toShortString()})\n" +
                plan.print()

    private fun Planner.Plan.print() =
        sessions.joinToString(separator = "\n") { "### ${it.print()}" }

    private fun Planner.Plan.Session.print() =
        "${printActivity()} on ${printTime()}\n" +
                printParticipants()

    private fun Planner.Plan.Session.printTime() =
        DiscordFormatter.timestamp(date.atNormalStartTime(), DiscordFormatter.TimestampFormat.LONG_DATE_TIME)

    private fun Planner.Plan.Session.printActivity(): String =
        DiscordFormatter.bold(configuration.getActivity(activityId).name)

    private fun Planner.Plan.Session.printParticipants() =
        configuration.getActivity(activityId).participants
            .map(Participant::userId)
            .sorted()
            .joinToString(
                separator = "\n",
                postfix = "\n"
            ) { userId ->
                attendees.firstOrNull { it.userId == userId }
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
