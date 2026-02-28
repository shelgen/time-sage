package com.github.shelgen.timesage.planning

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class Plan(val sessions: List<Session>) {
    val score: Score = Score(
        numberOfAttendees = sessions.asSequence().flatMap(Session::attendees).count { !it.ifNeedBe },
        numberOfIfNeedBeAttendees = sessions.asSequence().flatMap(Session::attendees).count { it.ifNeedBe },
        noTwoDaysInSequence = if (sessions.asSequence().drop(1)
                .mapIndexed { i, session ->
                    numDaysBetween(
                        latestDate = session.timeSlot.atOffset(ZoneOffset.UTC).toLocalDate(),
                        earliestDate = sessions[i].timeSlot.atOffset(ZoneOffset.UTC).toLocalDate()
                    )
                }.all { it >= 2 }
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
        val timeSlot: Instant,
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
