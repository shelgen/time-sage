package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.domain.AvailabilitiesWeek
import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.UserResponses
import com.github.shelgen.timesage.logger
import java.time.Instant
import java.time.LocalDate

class Planner(
    private val configuration: Configuration,
    private val datePeriod: DatePeriod,
    private val responses: UserResponses
) {
    constructor(
        configuration: Configuration,
        weekStartDate: LocalDate,
        week: AvailabilitiesWeek
    ) : this(configuration, DatePeriod.weekFrom(weekStartDate), week.responses)

    fun generatePossiblePlans(): List<Plan> {
        logger.info("Generating suggestions for period $datePeriod")
        val timeSlots = configuration.scheduling.getTimeSlots(datePeriod, configuration.timeZone)
        return findAllWeekPlansSortedByScore(timeSlots).toList()
    }

    private fun findAllWeekPlansSortedByScore(timeSlots: List<Instant>) =
        recursivelyFindPossiblePlans(planThusFar = emptyList(), remainingTimeSlots = timeSlots)
            .distinct()
            .filterNot(List<Plan.Session>::isEmpty)
            .filterNot(::isSuboptimalForAParticipant)
            .map(::Plan)
            .sortedBy(Plan::score)

    private fun isSuboptimalForAParticipant(plannedSessions: List<Plan.Session>) =
        configuration.activities.flatMap { it.participants }
            .asSequence()
            .filter { participant ->
                val userId = participant.userId
                plannedSessions.count { it.hasAttendee(userId) } < getSessionLimit(userId)
            }
            .any { participant ->
                val userId = participant.userId
                plannedSessions
                    .asSequence()
                    .filter { session ->
                        val activity = configuration.activities.first { it.id == session.activityId }
                        activity.hasParticipant(participant.userId)
                    }
                    .filterNot { it.hasAttendee(userId) }
                    .map(Plan.Session::timeSlot)
                    .map { getAvailability(userId, it) }
                    .any { it != AvailabilityStatus.UNAVAILABLE }
            }

    private fun getAvailability(userId: Long, timeSlot: Instant) =
        responses.forUserId(userId)?.availabilities?.forTimeSlot(timeSlot)
            ?: AvailabilityStatus.UNAVAILABLE

    private fun getSessionLimit(userId: Long) =
        responses.forUserId(userId)?.sessionLimit ?: 2

    private fun recursivelyFindPossiblePlans(
        planThusFar: List<Plan.Session> = emptyList(),
        remainingTimeSlots: List<Instant>
    ): Sequence<List<Plan.Session>> {
        val currentTimeSlot = remainingTimeSlots.firstOrNull()
        return if (currentTimeSlot == null) {
            sequenceOf(planThusFar)
        } else {
            configuration.activities
                .asSequence()
                .flatMap { activity ->
                    val potentialAttendees =
                        responses.map
                            .asSequence()
                            .map { (userId, response) ->
                                userId to (response.availabilities.forTimeSlot(currentTimeSlot)
                                    ?: AvailabilityStatus.UNAVAILABLE)
                            }
                            .filterNot { (_, availability) -> availability == AvailabilityStatus.UNAVAILABLE }
                            .map { (userId, availability) ->
                                Plan.Session.Attendee(
                                    userId = userId,
                                    ifNeedBe = availability == AvailabilityStatus.IF_NEED_BE
                                )
                            }
                            .filter { activity.hasParticipant(it.userId) }
                            .filterNot { attendee ->
                                val userId = attendee.userId
                                planThusFar.count { it.hasAttendee(userId) } == getSessionLimit(userId)
                            }
                            .toSet()

                    val requiredAttendees =
                        potentialAttendees
                            .filter { activity.isRequiredParticipant(it.userId) }
                            .toSet()

                    if (planThusFar.size < 2 && requiredAttendees.size == activity.participants.count { !it.optional }) {
                        potentialAttendees
                            .minus(requiredAttendees)
                            .combinations()
                            .filter { activity.participants.count { it.optional } - it.size <= activity.maxMissingOptionalParticipants }
                            .asSequence()
                            .flatMap { attendees ->
                                recursivelyFindPossiblePlans(
                                    planThusFar = planThusFar + Plan.Session(
                                        timeSlot = currentTimeSlot,
                                        activityId = activity.id,
                                        attendees = attendees + requiredAttendees,
                                        missingOptionalCount = activity.participants.count { it.optional } - attendees.size
                                    ),
                                    remainingTimeSlots = remainingTimeSlots.drop(1)
                                )
                            }
                    } else {
                        emptySequence()
                    }
                } + recursivelyFindPossiblePlans(
                planThusFar = planThusFar,
                remainingTimeSlots = remainingTimeSlots.drop(1)
            )
        }
    }

    companion object {
        fun <T> Set<T>.combinations(currentProgress: Set<T> = emptySet()): Set<Set<T>> = flatMap {
            val newSet = currentProgress + it
            setOf(newSet) + minus(it).combinations(newSet)
        }.plus(setOf(emptySet())).toSet()
    }
}
