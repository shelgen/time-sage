package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.PlannedSession

class AlternativePrinter(private val configuration: Configuration) {
    fun printAlternative(alternativeNumber: Int, plan: Plan) =
        "-# Alternative $alternativeNumber (${plan.score.toDisplayString()})\n" +
                plan.print()

    private fun Plan.print() =
        sessions.joinToString(separator = "\n") { "### ${it.print()}" }

    private fun PlannedSession.print() =
        "${printActivity()} on ${printTime()}\n" +
                printParticipants()

    private fun PlannedSession.printTime() =
        DiscordFormatter.timestamp(timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)

    private fun PlannedSession.printActivity(): String =
        DiscordFormatter.bold(configuration.getActivity(activityId).name)

    private fun PlannedSession.printParticipants() =
        configuration.getActivity(activityId).members
            .map(ActivityMember::userId)
            .sorted()
            .joinToString(
                separator = "\n",
                postfix = "\n"
            ) { userId ->
                participants.firstOrNull { it.userId == userId }
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
