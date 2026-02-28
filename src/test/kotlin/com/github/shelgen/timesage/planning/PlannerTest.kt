package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class PlannerTest {

    // UTC timezone to avoid DST complexity
    private val UTC = TimeZone.getTimeZone("UTC")

    // Week of 2025-08-04 (Mon) through 2025-08-10 (Sun)
    private val weekStart = LocalDate.parse("2025-08-04")

    // Time slots within that week at 18:00 UTC
    private val monday    = Instant.parse("2025-08-04T18:00:00Z")
    private val tuesday   = Instant.parse("2025-08-05T18:00:00Z")
    private val wednesday = Instant.parse("2025-08-06T18:00:00Z")
    private val thursday  = Instant.parse("2025-08-07T18:00:00Z")

    // Stable participant IDs used across scenarios
    private val alice = 1L
    private val bob   = 2L
    private val carol = 3L

    // ─── Plan generation ──────────────────────────────────────────────────────

    @Test
    fun `no activities produces no plans`() {
        val plans = planner(config()).generatePossiblePlans()
        assertTrue(plans.isEmpty())
    }

    @Test
    fun `all participants unavailable produces no plans`() {
        val conf = config(activity(1, alice to false, bob to false))
        val week = week(
            alice to response(monday to AvailabilityStatus.UNAVAILABLE),
            bob   to response(monday to AvailabilityStatus.UNAVAILABLE),
        )
        assertTrue(planner(conf, week).generatePossiblePlans().isEmpty())
    }

    @Test
    fun `required participant with no response produces no plans`() {
        // Bob has no response entry at all, which is treated as UNAVAILABLE for every slot
        val conf = config(activity(1, alice to false, bob to false))
        val week = week(alice to response(monday to AvailabilityStatus.AVAILABLE))
        assertTrue(planner(conf, week).generatePossiblePlans().isEmpty())
    }

    @Test
    fun `both required participants available generates exactly one single-session plan`() {
        val conf = config(activity(1, alice to false, bob to false))
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.AVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        assertEquals(1, plans.size)
        val session = plans.single().sessions.single()
        assertEquals(monday, session.timeSlot)
        assertEquals(1, session.activityId)
        assertTrue(session.hasAttendee(alice))
        assertTrue(session.hasAttendee(bob))
    }

    @Test
    fun `if-need-be availability is reflected in attendee flag`() {
        val conf = config(activity(1, alice to false, bob to false))
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.IF_NEED_BE),
        )

        val session = planner(conf, week).generatePossiblePlans().single().sessions.single()

        assertFalse(session.attendees.first { it.userId == alice }.ifNeedBe)
        assertTrue(session.attendees.first { it.userId == bob }.ifNeedBe)
    }

    @Test
    fun `optional participant is absent when unavailable and maxMissingOptionalParticipants allows it`() {
        val conf = config(activity(1, alice to false, bob to true, maxMissing = 1))
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.UNAVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        assertEquals(1, plans.size)
        val session = plans.single().sessions.single()
        assertTrue(session.hasAttendee(alice))
        assertFalse(session.hasAttendee(bob))
    }

    @Test
    fun `optional participant must attend when maxMissingOptionalParticipants is 0`() {
        // maxMissing=0 means no optional can be absent; only the plan including Bob is generated
        val conf = config(activity(1, alice to false, bob to true, maxMissing = 0))
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.AVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        assertEquals(1, plans.size)
        assertTrue(plans.single().sessions.single().hasAttendee(bob))
    }

    @Test
    fun `suboptimal plan filtered when available optional participant is excluded`() {
        // maxMissing=1 allows Bob to be absent from generation, but since he is available
        // any plan that excludes him would be suboptimal and must be filtered out
        val conf = config(activity(1, alice to false, bob to true, maxMissing = 1))
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.AVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        assertEquals(1, plans.size)
        assertTrue(plans.single().sessions.single().hasAttendee(bob))
    }

    @Test
    fun `session limit of 1 produces one plan per available time slot`() {
        val conf = config(
            activity(1, alice to false, bob to false),
            rules = listOf(
                TimeSlotRule(DayType.MONDAYS,    LocalTime.of(18, 0)),
                TimeSlotRule(DayType.WEDNESDAYS, LocalTime.of(18, 0)),
            ),
        )
        val week = week(
            alice to response(sessionLimit = 1, monday to AvailabilityStatus.AVAILABLE, wednesday to AvailabilityStatus.AVAILABLE),
            bob   to response(sessionLimit = 1, monday to AvailabilityStatus.AVAILABLE, wednesday to AvailabilityStatus.AVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        // Session limit prevents combining the two slots into one plan
        assertEquals(2, plans.size)
        assertTrue(plans.all { it.sessions.size == 1 })
        assertEquals(setOf(monday, wednesday), plans.map { it.sessions.single().timeSlot }.toSet())
    }

    @Test
    fun `two activities on separate time slots can form a combined plan`() {
        val conf = config(
            activity(1, alice to false, bob to false),
            activity(2, carol to false),
            rules = listOf(
                TimeSlotRule(DayType.MONDAYS,    LocalTime.of(18, 0)),
                TimeSlotRule(DayType.WEDNESDAYS, LocalTime.of(18, 0)),
            ),
        )
        // Alice and Bob only available Monday; Carol only available Wednesday
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.AVAILABLE),
            carol to response(wednesday to AvailabilityStatus.AVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        val twoSessionPlan = plans.first { it.sessions.size == 2 }
        assertTrue(twoSessionPlan.sessions.any { it.activityId == 1 && it.timeSlot == monday })
        assertTrue(twoSessionPlan.sessions.any { it.activityId == 2 && it.timeSlot == wednesday })
        // The combined plan has more total attendees (3) than either single-activity plan, so it ranks first
        assertEquals(2, plans.first().sessions.size)
    }

    // ─── Score ordering ───────────────────────────────────────────────────────

    @Test
    fun `score - more regular attendees always outrank fewer`() {
        // Monday: Alice (required) + Bob (optional, available) → 0 missing optional
        // Wednesday: Alice only, Bob unavailable → 1 missing optional
        // Session limit=1 so each slot produces its own plan
        val conf = config(
            activity(1, alice to false, bob to true, maxMissing = 1),
            rules = listOf(
                TimeSlotRule(DayType.MONDAYS,    LocalTime.of(18, 0)),
                TimeSlotRule(DayType.WEDNESDAYS, LocalTime.of(18, 0)),
            ),
        )
        val week = week(
            alice to response(sessionLimit = 1, monday to AvailabilityStatus.AVAILABLE, wednesday to AvailabilityStatus.AVAILABLE),
            bob   to response(sessionLimit = 1, monday to AvailabilityStatus.AVAILABLE, wednesday to AvailabilityStatus.UNAVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        assertEquals(2, plans.size)
        assertEquals(monday,    plans[0].sessions.single().timeSlot) // 0 missing → first
        assertEquals(wednesday, plans[1].sessions.single().timeSlot) // 1 missing → second
    }

    @Test
    fun `score - among equal missing attendees, fewer if-need-be ranks higher`() {
        // Activity: Alice (required), Bob (required), Carol (optional, maxMissing=1)
        // Wednesday: Alice+Bob available, Carol if-need-be → 0 missing, 1 if-need-be
        // Thursday:  Alice+Bob available, Carol unavailable → 1 missing, 0 if-need-be
        val conf = config(
            activity(1, alice to false, bob to false, carol to true, maxMissing = 1),
            rules = listOf(
                TimeSlotRule(DayType.MONDAYS,    LocalTime.of(18, 0)),
                TimeSlotRule(DayType.WEDNESDAYS, LocalTime.of(18, 0)),
                TimeSlotRule(DayType.THURSDAYS,  LocalTime.of(18, 0)),
            ),
        )
        // Wednesday: Alice+Bob available, Carol if-need-be → Score(2 regular, 1 if-need-be)
        // Thursday:  Alice+Bob available, Carol unavailable → Score(2 regular, 0 if-need-be)
        val week = week(
            alice to response(sessionLimit = 1, wednesday to AvailabilityStatus.AVAILABLE, thursday to AvailabilityStatus.AVAILABLE),
            bob   to response(sessionLimit = 1, wednesday to AvailabilityStatus.AVAILABLE, thursday to AvailabilityStatus.AVAILABLE),
            carol to response(sessionLimit = 1, wednesday to AvailabilityStatus.IF_NEED_BE, thursday to AvailabilityStatus.UNAVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        assertEquals(2, plans.size)
        assertEquals(wednesday, plans[0].sessions.single().timeSlot) // 0 missing + 1 if-need-be → first
        assertEquals(thursday,  plans[1].sessions.single().timeSlot) // 1 missing → second
    }

    @Test
    fun `score - plans with non-consecutive sessions rank above consecutive sessions`() {
        // Three slots: Mon, Tue, Thu; default session limit (2)
        // Among 2-session plans:
        //   Mon+Tue = 1 day apart → consecutive → noTwoDaysInSequence = 0
        //   Mon+Thu = 3 days apart → non-consecutive → noTwoDaysInSequence = 1
        //   Tue+Thu = 2 days apart → non-consecutive → noTwoDaysInSequence = 1
        val conf = config(
            activity(1, alice to false, bob to false),
            rules = listOf(
                TimeSlotRule(DayType.MONDAYS,   LocalTime.of(18, 0)),
                TimeSlotRule(DayType.TUESDAYS,  LocalTime.of(18, 0)),
                TimeSlotRule(DayType.THURSDAYS, LocalTime.of(18, 0)),
            ),
        )
        val week = week(
            alice to response(monday to AvailabilityStatus.AVAILABLE, tuesday to AvailabilityStatus.AVAILABLE, thursday to AvailabilityStatus.AVAILABLE),
            bob   to response(monday to AvailabilityStatus.AVAILABLE, tuesday to AvailabilityStatus.AVAILABLE, thursday to AvailabilityStatus.AVAILABLE),
        )

        val plans = planner(conf, week).generatePossiblePlans()

        fun planSlots(p: Plan) = p.sessions.map { it.timeSlot }.toSet()
        val monTuePlan = plans.first { planSlots(it) == setOf(monday, tuesday) }
        val monThuPlan = plans.first { planSlots(it) == setOf(monday, thursday) }
        val tueThuPlan = plans.first { planSlots(it) == setOf(tuesday, thursday) }

        // Verify scores
        assertEquals(1, monTuePlan.score.directlyFollowingDays)
        assertEquals(0, monThuPlan.score.directlyFollowingDays)
        assertEquals(0, tueThuPlan.score.directlyFollowingDays)

        // Non-consecutive 2-session plans must rank above the consecutive one
        assertTrue(plans.indexOf(monThuPlan) < plans.indexOf(monTuePlan), "Mon+Thu should outrank Mon+Tue")
        assertTrue(plans.indexOf(tueThuPlan) < plans.indexOf(monTuePlan), "Tue+Thu should outrank Mon+Tue")
    }

    // ─── Score unit tests ─────────────────────────────────────────────────────

    @Test
    fun `Score ordering - missing first, then if-need-be, then more sessions, then more participants, then fewer consecutive days`() {
        // s1: 0 missing, 0 if-need-be, 2 sessions, 6 participants, 0 following → best
        val s1 = Plan.Score(missingOptionalAttendees = 0, ifNeedBeAttendees = 0, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 0)
        // s2: 0 missing, 0 if-need-be, 2 sessions, 6 participants, 1 following → worse than s1
        val s2 = Plan.Score(missingOptionalAttendees = 0, ifNeedBeAttendees = 0, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 1)
        // s3: 0 missing, 0 if-need-be, 2 sessions, 4 participants, 0 following → fewer participants than s1
        val s3 = Plan.Score(missingOptionalAttendees = 0, ifNeedBeAttendees = 0, numberOfSessions = 2, participantSessions = 4, directlyFollowingDays = 0)
        // s4: 0 missing, 0 if-need-be, 1 session, 3 participants, 0 following → fewer sessions
        val s4 = Plan.Score(missingOptionalAttendees = 0, ifNeedBeAttendees = 0, numberOfSessions = 1, participantSessions = 3, directlyFollowingDays = 0)
        // s5: 0 missing, 1 if-need-be, 2 sessions, 6 participants, 0 following → has if-need-be
        val s5 = Plan.Score(missingOptionalAttendees = 0, ifNeedBeAttendees = 1, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 0)
        // s6: 1 missing, 0 if-need-be, 2 sessions, 6 participants, 0 following → has missing → worst
        val s6 = Plan.Score(missingOptionalAttendees = 1, ifNeedBeAttendees = 0, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 0)

        assertEquals(listOf(s1, s2, s3, s4, s5, s6), listOf(s6, s4, s2, s5, s3, s1).sorted())
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun planner(
        configuration: Configuration,
        week: AvailabilitiesWeek = AvailabilitiesWeek.DEFAULT,
    ) = Planner(configuration = configuration, weekStartDate = weekStart, week = week)

    private fun config(
        vararg activities: Activity,
        rules: List<TimeSlotRule> = listOf(TimeSlotRule(DayType.MONDAYS, LocalTime.of(18, 0))),
    ) = Configuration(
        enabled = true,
        timeZone = UTC,
        scheduling = Scheduling(
            type = SchedulingType.WEEKLY,
            startDayOfWeek = DayOfWeek.MONDAY,
            timeSlotRules = rules,
        ),
        activities = activities.toList(),
        voiceChannelId = null,
    )

    private fun activity(id: Int, vararg participants: Pair<Long, Boolean>, maxMissing: Int = 0) =
        Activity(
            id = id,
            name = "Activity $id",
            participants = participants.map { (userId, optional) -> Participant(userId, optional) },
            maxMissingOptionalParticipants = maxMissing,
        )

    private fun week(vararg responses: Pair<Long, UserResponse>) = AvailabilitiesWeek(
        messageId = null,
        responses = UserResponses(responses.toMap()),
        concluded = false,
        conclusionMessageId = null,
    )

    private fun response(vararg availabilities: Pair<Instant, AvailabilityStatus>) =
        UserResponse(
            sessionLimit = null,
            availabilities = DateAvailabilities(availabilities.toMap()),
        )

    private fun response(sessionLimit: Int, vararg availabilities: Pair<Instant, AvailabilityStatus>) =
        UserResponse(
            sessionLimit = sessionLimit,
            availabilities = DateAvailabilities(availabilities.toMap()),
        )
}
