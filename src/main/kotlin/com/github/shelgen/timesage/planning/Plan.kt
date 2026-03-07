package com.github.shelgen.timesage.planning

import java.time.LocalDate
import java.time.ZoneOffset

data class Plan(val sessions: List<PlannedSession>) {
    val score: Score = Score(
        missingOptionalParticipants = sessions.sumOf { it.missingOptionalCount },
        ifNeedBeParticipants = sessions.sumOf { session -> session.participants.count { it.ifNeedBe } },
        numberOfSessions = sessions.size,
        participantSessions = sessions.sumOf { it.participants.size },
        directlyFollowingDays = sessions.zipWithNext().count { (a, b) ->
            numDaysBetween(
                latestDate = b.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate(),
                earliestDate = a.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate()
            ) == 1L
        }
    )

    data class Score(
        val missingOptionalParticipants: Int,
        val ifNeedBeParticipants: Int,
        val numberOfSessions: Int,
        val participantSessions: Int,
        val directlyFollowingDays: Int
    ) : Comparable<Score> {
        private val comparator: Comparator<Score> = compareBy(
            { it.missingOptionalParticipants },
            { it.ifNeedBeParticipants },
            { -it.numberOfSessions },
            { -it.participantSessions },
            { it.directlyFollowingDays }
        )

        override fun compareTo(other: Score) = comparator.compare(this, other)

        fun toDisplayString(): String {
            val parts = buildList {
                if (missingOptionalParticipants > 0)
                    add("$missingOptionalParticipants missing")
                if (ifNeedBeParticipants > 0)
                    add("$ifNeedBeParticipants if-need-be")
                add(if (numberOfSessions == 1) "1 session" else "$numberOfSessions sessions")
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
