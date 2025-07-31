package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.Week
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.weekDatesStartingWith
import java.time.LocalDate

class Planner(
    private val configuration: Configuration,
    private val week: Week
) {
    fun generatePossiblePlans(): List<Plan> {
        logger.info("Generating suggestions for week starting ${week.startDate}")
        return findAllWeekPlansSortedByScore(weekDatesStartingWith(week.startDate)).toList()
    }

    private fun findAllWeekPlansSortedByScore(weekDates: List<LocalDate>) =
        recursivelyFindPossiblePlans(planThusFar = emptyList(), remainingSlots = weekDates)
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
                    .map(Plan.Session::date)
                    .map { getAvailability(userId, it) }
                    .any { it != AvailabilityStatus.UNAVAILABLE }
            }

    private fun getAvailability(userId: Long, date: LocalDate) =
        week.responses[userId]?.availability[date]
            ?: AvailabilityStatus.UNAVAILABLE

    private fun getSessionLimit(userId: Long) =
        week.responses[userId]?.sessionLimit ?: 2

    private fun recursivelyFindPossiblePlans(
        planThusFar: List<Plan.Session> = emptyList(),
        remainingSlots: List<LocalDate>
    ): Sequence<List<Plan.Session>> {
        val currentDate = remainingSlots.firstOrNull()
        return if (currentDate == null) {
            sequenceOf(planThusFar)
        } else {
            configuration.activities
                .asSequence()
                .flatMap { activity ->
                    val potentialAttendees =
                        week.responses
                            .asSequence()
                            .map { (userId, response) ->
                                userId to (response.availability[currentDate]
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
                            .permutations()
                            .filter { activity.participants.count { it.optional } - it.size <= activity.maxMissingOptionalParticipants }
                            .asSequence()
                            .flatMap { attendees ->
                                recursivelyFindPossiblePlans(
                                    planThusFar = planThusFar + Plan.Session(
                                        date = currentDate,
                                        activityId = activity.id,
                                        attendees = attendees + requiredAttendees
                                    ),
                                    remainingSlots = remainingSlots.drop(1)
                                )
                            }
                    } else {
                        emptySequence()
                    }
                } + recursivelyFindPossiblePlans(
                planThusFar = planThusFar,
                remainingSlots = remainingSlots.drop(1)
            )
        }
    }

    data class Plan(val sessions: List<Session>) {
        val score: Score = Score(
            numberOfAttendees = sessions.asSequence().flatMap(Session::attendees).count { !it.ifNeedBe },
            numberOfIfNeedBeAttendees = sessions.asSequence().flatMap(Session::attendees).count { it.ifNeedBe },
            noTwoDaysInSequence = if (sessions.asSequence().drop(1)
                    .mapIndexed { i, session -> numDaysBetween(session.date, sessions[i].date) }.all { it >= 2 }
            ) 1 else 0
        )

        data class Score(
            val numberOfAttendees: Int,
            val numberOfIfNeedBeAttendees: Int,
            val noTwoDaysInSequence: Int
        ) : Comparable<Score> {
            private val comparator: Comparator<Score> =
                Comparator.comparingInt(Score::numberOfAttendees)
                    .thenComparingInt(Score::numberOfIfNeedBeAttendees)
                    .thenComparingInt(Score::noTwoDaysInSequence)
                    .reversed()

            override fun compareTo(other: Score) = comparator.compare(this, other)

            fun toShortString() = "$numberOfAttendees.$numberOfIfNeedBeAttendees.$noTwoDaysInSequence"
        }

        data class Session(
            val date: LocalDate,
            val activityId: Int,
            val attendees: Set<Attendee>
        ) {
            data class Attendee(val userId: Long, val ifNeedBe: Boolean)

            private val attendeeUserIds: Set<Long> = attendees.map(Attendee::userId).toSet()

            fun hasAttendee(userId: Long) = userId in attendeeUserIds
        }

        private fun numDaysBetween(latestDate: LocalDate, earliestDate: LocalDate) =
            latestDate.toEpochDay() - earliestDate.toEpochDay()
    }

    companion object {
        fun <T> Set<T>.permutations(currentProgress: Set<T> = emptySet()): Set<Set<T>> = flatMap {
            val newSet = currentProgress + it
            setOf(newSet) + minus(it).permutations(newSet)
        }.toSet()
    }
}
