package com.github.shelgen.timesage.plan

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

data class PlanId(val value: UUID)

data class Plan(
    val id: PlanId,
    val sessions: List<Session>
) {
    val score: Score = Score(
        missingOptionalParticipants = sessions.sumOf(Session::missingOptionalCount),
        numberOfSessions = sessions.size,
        ifNeedBeParticipants = sessions.sumOf(Session::countIfNeedBes),
        participantSessions = sessions.sumOf(Session::countParticipants),
        directlyFollowingDays = sessions.zipWithNext().count { (a, b) ->
            numDaysBetween(
                latestDate = b.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate(),
                earliestDate = a.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate()
            ) == 1L
        }
    )

    data class Score(
        val missingOptionalParticipants: Int,
        val numberOfSessions: Int,
        val ifNeedBeParticipants: Int,
        val participantSessions: Int,
        val directlyFollowingDays: Int
    ) : Comparable<Score> {
        private val comparator: Comparator<Score> = compareBy(
            { it.missingOptionalParticipants },
            { -it.numberOfSessions },
            { it.ifNeedBeParticipants },
            { -it.participantSessions },
            { it.directlyFollowingDays }
        )

        override fun compareTo(other: Score) = comparator.compare(this, other)

        fun toDisplayString(): String {
            val parts = buildList {
                if (missingOptionalParticipants > 0)
                    add("$missingOptionalParticipants missing")
                add(if (numberOfSessions == 1) "1 session" else "$numberOfSessions sessions")
                if (ifNeedBeParticipants > 0)
                    add("$ifNeedBeParticipants if-need-be")
                add(if (participantSessions == 1) "1 participant" else "$participantSessions participants")
                if (directlyFollowingDays > 0)
                    add(if (directlyFollowingDays == 1) "1 directly following day" else "$directlyFollowingDays directly following days")
            }
            return parts.joinToString(", ")
        }
    }

    private fun numDaysBetween(latestDate: LocalDate, earliestDate: LocalDate) =
        latestDate.toEpochDay() - earliestDate.toEpochDay()
}
