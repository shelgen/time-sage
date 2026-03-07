package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.UserResponses
import com.github.shelgen.timesage.logger
import java.time.Instant

class Planner(
    private val configuration: Configuration,
    private val dateRange: DateRange,
    private val responses: UserResponses
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
            .flatMap { it.participants }
            .any { participant ->
                val userId = participant.userId
                sessions.count { it.hasAttendee(userId) } < sessionLimit(userId) &&
                        wasExcludedFromAnAvailableSession(userId, sessions)
            }

    private fun wasExcludedFromAnAvailableSession(userId: Long, sessions: List<Plan.Session>): Boolean =
        sessions
            .filter { session ->
                val activity = configuration.activities.first { it.id == session.activityId }
                activity.hasParticipant(userId)
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
        val requiredAttendees = availableAttendees.filter { activity.isRequiredParticipant(it.userId) }.toSet()
        val optionalAttendees = availableAttendees - requiredAttendees

        val allRequiredPresent = requiredAttendees.size == activity.participants.count { !it.optional }
        if (!allRequiredPresent) return emptySequence()

        val totalOptional = activity.participants.count { it.optional }

        return optionalAttendees
            .combinations()
            .filter { subset -> totalOptional - subset.size <= activity.maxMissingOptionalParticipants }
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
        responses.map
            .asSequence()
            .filter { (userId, _) -> activity.hasParticipant(userId) }
            .filter { (userId, _) -> plannedSessions.count { it.hasAttendee(userId) } < sessionLimit(userId) }
            .mapNotNull { (userId, response) ->
                val availability = response.availabilities[timeSlot] ?: AvailabilityStatus.UNAVAILABLE
                if (availability == AvailabilityStatus.UNAVAILABLE) null
                else Plan.Session.Attendee(userId, ifNeedBe = availability == AvailabilityStatus.IF_NEED_BE)
            }
            .toSet()

    private fun availabilityFor(userId: Long, timeSlot: Instant): AvailabilityStatus =
        responses[userId]?.availabilities?.get(timeSlot) ?: AvailabilityStatus.UNAVAILABLE

    private fun sessionLimit(userId: Long): Int =
        responses[userId]?.sessionLimit ?: 2

    companion object {
        fun <T> Set<T>.combinations(): Set<Set<T>> {
            if (isEmpty()) return setOf(emptySet())
            val first = first()
            val rest = minus(first).combinations()
            return rest + rest.map { it + first }
        }
    }
}
