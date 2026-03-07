package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.AvailabilityResponses
import com.github.shelgen.timesage.logger
import java.time.Instant

class Planner(
    private val configuration: Configuration,
    private val dateRange: DateRange,
    private val availabilityResponses: AvailabilityResponses
) {
    fun generatePossiblePlans(): List<Plan> {
        logger.info("Generating possible plans for period $dateRange")
        val timeSlots = configuration.scheduling.timeSlotRules.getTimeSlots(dateRange, configuration.localization.timeZone)
        return buildSessions(plannedSessions = emptyList(), remainingTimeSlots = timeSlots)
            .distinct()
            .filter { it.isNotEmpty() }
            .filterNot { isSuboptimalForAParticipant(it) }
            .map(::Plan)
            .sortedBy(Plan::score)
            .toList()
    }

    // Returns true if any participant could have been scheduled for more sessions but wasn't.
    private fun isSuboptimalForAParticipant(sessions: List<Plan.Session>): Boolean =
        configuration.activities
            .flatMap { it.members }
            .any { participant ->
                val userId = participant.userId
                sessions.count { it.hasAttendee(userId) } < sessionLimit(userId) &&
                        wasExcludedFromAnAvailableSession(userId, sessions)
            }

    private fun wasExcludedFromAnAvailableSession(userId: Long, sessions: List<Plan.Session>): Boolean =
        sessions
            .filter { session ->
                val activity = configuration.activities.first { it.id == session.activityId }
                activity.userIsMember(userId)
            }
            .filterNot { it.hasAttendee(userId) }
            .any { session -> availabilityFor(userId, session.timeSlot) != AvailabilityStatus.UNAVAILABLE }

    private fun buildSessions(
        plannedSessions: List<Plan.Session>,
        remainingTimeSlots: List<Instant>
    ): Sequence<List<Plan.Session>> {
        val currentSlot = remainingTimeSlots.firstOrNull()
            ?: return sequenceOf(plannedSessions)
        val nextSlots = remainingTimeSlots.drop(1)

        val withSessionInCurrentSlot = configuration.activities
            .asSequence()
            .flatMap { activity -> candidateSessionsFor(activity, currentSlot, plannedSessions, nextSlots) }
        val withSlotSkipped = buildSessions(plannedSessions, nextSlots)

        return withSessionInCurrentSlot + withSlotSkipped
    }

    private fun candidateSessionsFor(
        activity: Activity,
        timeSlot: Instant,
        plannedSessions: List<Plan.Session>,
        nextSlots: List<Instant>
    ): Sequence<List<Plan.Session>> {
        if (plannedSessions.size >= 2) return emptySequence()

        val availableAttendees = availableAttendeesFor(activity, timeSlot, plannedSessions)
        val requiredAttendees = availableAttendees.filter { activity.userIsRequiredMember(it.userId) }.toSet()
        val optionalAttendees = availableAttendees - requiredAttendees

        val allRequiredPresent = requiredAttendees.size == activity.members.count { !it.optional }
        if (!allRequiredPresent) return emptySequence()

        val totalOptional = activity.members.count { it.optional }

        return optionalAttendees
            .combinations()
            .filter { subset -> totalOptional - subset.size <= activity.maxMissingOptionalMembers }
            .asSequence()
            .flatMap { optionalSubset ->
                val session = Plan.Session(
                    timeSlot = timeSlot,
                    activityId = activity.id,
                    attendees = requiredAttendees + optionalSubset,
                    missingOptionalCount = totalOptional - optionalSubset.size
                )
                buildSessions(plannedSessions + session, nextSlots)
            }
    }

    private fun availableAttendeesFor(
        activity: Activity,
        timeSlot: Instant,
        plannedSessions: List<Plan.Session>
    ): Set<Plan.Session.Attendee> =
        availabilityResponses.map
            .asSequence()
            .filter { (userId, _) -> activity.userIsMember(userId) }
            .filter { (userId, _) -> plannedSessions.count { it.hasAttendee(userId) } < sessionLimit(userId) }
            .mapNotNull { (userId, response) ->
                val availability = response.dates[timeSlot] ?: AvailabilityStatus.UNAVAILABLE
                if (availability == AvailabilityStatus.UNAVAILABLE) null
                else Plan.Session.Attendee(userId, ifNeedBe = availability == AvailabilityStatus.IF_NEED_BE)
            }
            .toSet()

    private fun availabilityFor(userId: Long, timeSlot: Instant): AvailabilityStatus =
        availabilityResponses[userId]?.dates?.get(timeSlot) ?: AvailabilityStatus.UNAVAILABLE

    private fun sessionLimit(userId: Long): Int =
        availabilityResponses[userId]?.sessionLimit ?: 2

    companion object {
        fun <T> Set<T>.combinations(): Set<Set<T>> {
            if (isEmpty()) return setOf(emptySet())
            val first = first()
            val rest = minus(first).combinations()
            return rest + rest.map { it + first }
        }
    }
}
