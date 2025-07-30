package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.domain.AvailabilityStatus
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.Week
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.weekDatesForMonday
import java.time.LocalDate

class Planner(
    private val configuration: Configuration,
    private val week: Week
) {
    fun generatePossiblePlans(): List<Plan> {
        logger.info("Generating suggestions for week of Monday ${week.mondayDate}")
        return findAllWeekPlansSortedByScore(weekDatesForMonday(week.mondayDate)).toList()
    }

    private fun findAllWeekPlansSortedByScore(weekDates: List<LocalDate>) =
        recursivelyFindPossiblePlans(planThusFar = emptyList(), remainingSlots = weekDates)
            .distinct()
            .filterNot(List<Plan.Session>::isEmpty)
            .filterNot(::isSuboptimalForAPlayer)
            .map(::Plan)
            .sortedBy(Plan::score)

    private fun isSuboptimalForAPlayer(plannedSessions: List<Plan.Session>) =
        configuration.campaigns.flatMap { it.gmDiscordIds + it.playerDiscordIds }.distinct()
            .asSequence()
            .filter { playerUserId ->
                plannedSessions.count { it.hasAttendee(playerUserId) } <
                        getSessionLimit(playerUserId)
            }
            .any { playerUserId ->
                plannedSessions
                    .asSequence()
                    .filter { session ->
                        val campaign = configuration.campaigns.first { it.id == session.campaignId }
                        playerUserId in campaign.gmDiscordIds || playerUserId in campaign.playerDiscordIds
                    }
                    .filterNot { it.hasAttendee(playerUserId) }
                    .map(Plan.Session::date)
                    .map { getAvailability(playerUserId, it) }
                    .any { it != AvailabilityStatus.UNAVAILABLE }
            }

    private fun getAvailability(playerUserId: Long, date: LocalDate) =
        week.playerResponses[playerUserId]?.availability[date]
            ?: AvailabilityStatus.UNAVAILABLE

    private fun getSessionLimit(playerUserId: Long) =
        week.playerResponses[playerUserId]?.sessionLimit ?: 2

    private fun recursivelyFindPossiblePlans(
        planThusFar: List<Plan.Session> = emptyList(),
        remainingSlots: List<LocalDate>
    ): Sequence<List<Plan.Session>> {
        val currentDate = remainingSlots.firstOrNull()
        return if (currentDate == null) {
            sequenceOf(planThusFar)
        } else {
            configuration.campaigns
                .asSequence()
                .flatMap { campaign ->
                    val potentialAttendees =
                        week.playerResponses
                            .asSequence()
                            .map { (playerUserId, response) ->
                                playerUserId to (response.availability[currentDate]
                                    ?: AvailabilityStatus.UNAVAILABLE)
                            }
                            .filterNot { (_, availability) -> availability == AvailabilityStatus.UNAVAILABLE }
                            .map { (playerUserId, availability) ->
                                Plan.Session.Attendee(
                                    playerDiscordId = playerUserId,
                                    ifNeedBe = availability == AvailabilityStatus.IF_NEED_BE
                                )
                            }
                            .filter {
                                val playerUserId = it.playerDiscordId
                                playerUserId in campaign.gmDiscordIds || playerUserId in campaign.playerDiscordIds
                            }
                            .filterNot { attendee ->
                                val playerUserId = attendee.playerDiscordId
                                planThusFar.count { it.hasAttendee(playerUserId) } == getSessionLimit(playerUserId)
                            }
                            .toSet()

                    val gmAttendees = potentialAttendees.filter { it.playerDiscordId in campaign.gmDiscordIds }.toSet()

                    if (planThusFar.size < 2 && gmAttendees.size == campaign.gmDiscordIds.size) {
                        potentialAttendees
                            .minus(gmAttendees)
                            .permutations()
                            .filter { campaign.playerDiscordIds.size - it.size <= campaign.maxNumMissingPlayers }
                            .asSequence()
                            .flatMap { attendees ->
                                recursivelyFindPossiblePlans(
                                    planThusFar = planThusFar + Plan.Session(
                                        date = currentDate,
                                        campaignId = campaign.id,
                                        attendees = attendees + gmAttendees
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
            val campaignId: Int,
            val attendees: Set<Attendee>
        ) {
            data class Attendee(val playerDiscordId: Long, val ifNeedBe: Boolean)

            private val attendingPlayers: Set<Long> = attendees.map(Attendee::playerDiscordId).toSet()

            fun hasAttendee(playerDiscordId: Long) = playerDiscordId in attendingPlayers
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
