package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.plan.Participant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.plan.Session
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.time.TimeSlot
import java.util.*

class Planner(
    private val configuration: Configuration,
    private val dateRange: DateRange,
    private val planningProcess: PlanningProcess,
) {
    fun generatePossiblePlans(): List<Plan> {
        logger.info("Generating possible plans for period $dateRange")
        val timeSlots = configuration.produceTimeSlots(dateRange)
        return buildSessions(sessions = emptyList(), remainingTimeSlots = timeSlots)
            .distinct()
            .filter { it.isNotEmpty() }
            .filterNot { isSuboptimalForAParticipant(it) }
            .map { Plan(PlanId(UUID.randomUUID()), it) }
            .sortedBy(Plan::score)
            .toList()
    }

    // Returns true if any participant could have been scheduled for more sessions but wasn't.
    private fun isSuboptimalForAParticipant(sessions: List<Session>): Boolean =
        configuration.activities
            .flatMap { it.members }
            .any { participant ->
                val user = participant.user
                sessions.count { it.hasParticipant(user) } < sessionLimit(user) &&
                        wasExcludedFromAnAvailableSession(user, sessions)
            }

    private fun wasExcludedFromAnAvailableSession(user: DiscordUserId, sessions: List<Session>): Boolean =
        sessions
            .filter { session ->
                val activity = configuration.activities.first { it.id == session.activityId }
                activity.isMember(user)
            }
            .filterNot { it.hasParticipant(user) }
            .any { session -> availabilityFor(user, session.timeSlot) != Availability.UNAVAILABLE }

    private fun buildSessions(
        sessions: List<Session>,
        remainingTimeSlots: List<TimeSlot>
    ): Sequence<List<Session>> {
        val currentSlot = remainingTimeSlots.firstOrNull()
            ?: return sequenceOf(sessions)
        val nextSlots = remainingTimeSlots.drop(1)

        val withSessionInCurrentSlot = configuration.activities
            .asSequence()
            .flatMap { activity -> candidateSessionsFor(activity, currentSlot, sessions, nextSlots) }
        val withSlotSkipped = buildSessions(sessions, nextSlots)

        return withSessionInCurrentSlot + withSlotSkipped
    }

    private fun candidateSessionsFor(
        activity: Activity,
        timeSlot: TimeSlot,
        sessions: List<Session>,
        nextSlots: List<TimeSlot>
    ): Sequence<List<Session>> {
        val availableParticipants = availableParticipantsFor(activity, timeSlot, sessions)
        val requiredParticipants = availableParticipants.filter { activity.isRequiredMember(it.user) }.toSet()
        val optionalParticipants = availableParticipants - requiredParticipants

        val allRequiredPresent = requiredParticipants.size == activity.members.count { !it.optional }
        if (!allRequiredPresent) return emptySequence()

        val totalOptional = activity.members.count { it.optional }

        return optionalParticipants
            .combinations()
            .filter { subset -> totalOptional - subset.size <= activity.maxNumMissingOptionalMembers }
            .asSequence()
            .flatMap { optionalSubset ->
                val session = Session(
                    timeSlot = timeSlot,
                    activityId = activity.id,
                    participants = requiredParticipants + optionalSubset,
                    missingOptionalCount = totalOptional - optionalSubset.size
                )
                buildSessions(sessions + session, nextSlots)
            }
    }

    private fun availableParticipantsFor(
        activity: Activity,
        timeSlot: TimeSlot,
        sessions: List<Session>
    ): Set<Participant> =
        planningProcess.availabilityResponses
            .asSequence()
            .filter { (user, _) -> activity.isMember(user) }
            .filter { (user, _) -> sessions.count { it.hasParticipant(user) } < sessionLimit(user) }
            .mapNotNull { (user, response) ->
                when (response[timeSlot]) {
                    Availability.AVAILABLE -> Participant(user, ifNeedBe = false)
                    Availability.IF_NEED_BE -> Participant(user, ifNeedBe = true)
                    Availability.UNAVAILABLE, null -> null
                }
            }
            .toSet()

    private fun availabilityFor(user: DiscordUserId, timeSlot: TimeSlot): Availability =
        planningProcess.availabilityResponses[user]?.get(timeSlot) ?: Availability.UNAVAILABLE

    private fun sessionLimit(user: DiscordUserId): Int =
        planningProcess.availabilityResponses[user]?.sessionLimit ?: 2

    companion object {
        fun <T> Set<T>.combinations(): Set<Set<T>> {
            if (isEmpty()) return setOf(emptySet())
            val first = first()
            val rest = minus(first).combinations()
            return rest + rest.map { it + first }
        }
    }
}
