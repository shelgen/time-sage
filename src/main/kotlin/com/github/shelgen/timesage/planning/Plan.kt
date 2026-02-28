package com.github.shelgen.timesage.planning

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class Plan(val sessions: List<Session>) {
    val score: Score = Score(
        missingOptionalAttendees = sessions.sumOf { it.missingOptionalCount },
        ifNeedBeAttendees = sessions.sumOf { session -> session.attendees.count { it.ifNeedBe } },
        numberOfSessions = sessions.size,
        participantSessions = sessions.sumOf { it.attendees.size },
        directlyFollowingDays = sessions.zipWithNext().count { (a, b) ->
            numDaysBetween(
                latestDate = b.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate(),
                earliestDate = a.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate()
            ) == 1L
        }
    )

    data class Score(
        val missingOptionalAttendees: Int,
        val ifNeedBeAttendees: Int,
        val numberOfSessions: Int,
        val participantSessions: Int,
        val directlyFollowingDays: Int
    ) : Comparable<Score> {
        private val comparator: Comparator<Score> = compareBy(
            { it.missingOptionalAttendees },
            { it.ifNeedBeAttendees },
            { -it.numberOfSessions },
            { -it.participantSessions },
            { it.directlyFollowingDays }
        )

        override fun compareTo(other: Score) = comparator.compare(this, other)

        fun toDisplayString(): String {
            val parts = buildList {
                if (missingOptionalAttendees > 0)
                    add("$missingOptionalAttendees missing")
                if (ifNeedBeAttendees > 0)
                    add("$ifNeedBeAttendees if-need-be")
                add(if (numberOfSessions == 1) "1 session" else "$numberOfSessions sessions")
                add(if (participantSessions == 1) "1 participant" else "$participantSessions participants")
                if (directlyFollowingDays > 0)
                    add(if (directlyFollowingDays == 1) "1 directly following day" else "$directlyFollowingDays directly following days")
            }
            return parts.joinToString(", ")
        }
    }

    data class Session(
        val timeSlot: Instant,
        val activityId: Int,
        val attendees: Set<Attendee>,
        val missingOptionalCount: Int
    ) {
        data class Attendee(val userId: Long, val ifNeedBe: Boolean)

        private val attendeeUserIds: Set<Long> = attendees.map(Attendee::userId).toSet()

        fun hasAttendee(userId: Long) = userId in attendeeUserIds
    }

    private fun numDaysBetween(latestDate: LocalDate, earliestDate: LocalDate) =
        latestDate.toEpochDay() - earliestDate.toEpochDay()
}
