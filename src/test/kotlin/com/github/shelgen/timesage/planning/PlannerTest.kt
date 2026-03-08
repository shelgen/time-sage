package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Localization
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.configuration.PeriodicPlanning
import com.github.shelgen.timesage.configuration.Reminders
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.time.DateRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

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
        val responses = responses(
            alice to response(monday to Availability.UNAVAILABLE),
            bob   to response(monday to Availability.UNAVAILABLE),
        )
        assertTrue(planner(conf, responses).generatePossiblePlans().isEmpty())
    }

    @Test
    fun `required participant with no response produces no plans`() {
        // Bob has no response entry at all, which is treated as UNAVAILABLE for every slot
        val conf = config(activity(1, alice to false, bob to false))
        val responses = responses(alice to response(monday to Availability.AVAILABLE))
        assertTrue(planner(conf, responses).generatePossiblePlans().isEmpty())
    }

    @Test
    fun `both required participants available generates exactly one single-session plan`() {
        val conf = config(activity(1, alice to false, bob to false))
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE),
            bob   to response(monday to Availability.AVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        assertEquals(1, plans.size)
        val session = plans.single().sessions.single()
        assertEquals(monday, session.timeSlot)
        assertEquals(ActivityId(1), session.activityId)
        assertTrue(session.hasParticipant(DiscordUserId(alice)))
        assertTrue(session.hasParticipant(DiscordUserId(bob)))
    }

    @Test
    fun `if-need-be availability is reflected in attendee flag`() {
        val conf = config(activity(1, alice to false, bob to false))
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE),
            bob   to response(monday to Availability.IF_NEED_BE),
        )

        val session = planner(conf, responses).generatePossiblePlans().single().sessions.single()

        assertFalse(session.participants.first { it.user == DiscordUserId(alice) }.ifNeedBe)
        assertTrue(session.participants.first { it.user == DiscordUserId(bob) }.ifNeedBe)
    }

    @Test
    fun `optional participant is absent when unavailable and maxMissingOptionalParticipants allows it`() {
        val conf = config(activity(1, alice to false, bob to true, maxMissing = 1))
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE),
            bob   to response(monday to Availability.UNAVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        assertEquals(1, plans.size)
        val session = plans.single().sessions.single()
        assertTrue(session.hasParticipant(DiscordUserId(alice)))
        assertFalse(session.hasParticipant(DiscordUserId(bob)))
    }

    @Test
    fun `optional participant must attend when maxMissingOptionalParticipants is 0`() {
        // maxMissing=0 means no optional can be absent; only the plan including Bob is generated
        val conf = config(activity(1, alice to false, bob to true, maxMissing = 0))
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE),
            bob   to response(monday to Availability.AVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        assertEquals(1, plans.size)
        assertTrue(plans.single().sessions.single().hasParticipant(DiscordUserId(bob)))
    }

    @Test
    fun `suboptimal plan filtered when available optional participant is excluded`() {
        // maxMissing=1 allows Bob to be absent from generation, but since he is available
        // any plan that excludes him would be suboptimal and must be filtered out
        val conf = config(activity(1, alice to false, bob to true, maxMissing = 1))
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE),
            bob   to response(monday to Availability.AVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        assertEquals(1, plans.size)
        assertTrue(plans.single().sessions.single().hasParticipant(DiscordUserId(bob)))
    }

    @Test
    fun `session limit of 1 produces one plan per available time slot`() {
        val conf = config(
            activity(1, alice to false, bob to false),
            rules = mapOf(
                DayOfWeek.MONDAY    to LocalTime.of(18, 0),
                DayOfWeek.WEDNESDAY to LocalTime.of(18, 0),
            ),
        )
        val responses = responses(
            alice to response(sessionLimit = 1, monday to Availability.AVAILABLE, wednesday to Availability.AVAILABLE),
            bob   to response(sessionLimit = 1, monday to Availability.AVAILABLE, wednesday to Availability.AVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

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
            rules = mapOf(
                DayOfWeek.MONDAY    to LocalTime.of(18, 0),
                DayOfWeek.WEDNESDAY to LocalTime.of(18, 0),
            ),
        )
        // Alice and Bob only available Monday; Carol only available Wednesday
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE),
            bob   to response(monday to Availability.AVAILABLE),
            carol to response(wednesday to Availability.AVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        val twoSessionPlan = plans.first { it.sessions.size == 2 }
        assertTrue(twoSessionPlan.sessions.any { it.activityId == ActivityId(1) && it.timeSlot == monday })
        assertTrue(twoSessionPlan.sessions.any { it.activityId == ActivityId(2) && it.timeSlot == wednesday })
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
            rules = mapOf(
                DayOfWeek.MONDAY    to LocalTime.of(18, 0),
                DayOfWeek.WEDNESDAY to LocalTime.of(18, 0),
            ),
        )
        val responses = responses(
            alice to response(sessionLimit = 1, monday to Availability.AVAILABLE, wednesday to Availability.AVAILABLE),
            bob   to response(sessionLimit = 1, monday to Availability.AVAILABLE, wednesday to Availability.UNAVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

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
            rules = mapOf(
                DayOfWeek.MONDAY    to LocalTime.of(18, 0),
                DayOfWeek.WEDNESDAY to LocalTime.of(18, 0),
                DayOfWeek.THURSDAY  to LocalTime.of(18, 0),
            ),
        )
        val responses = responses(
            alice to response(sessionLimit = 1, wednesday to Availability.AVAILABLE, thursday to Availability.AVAILABLE),
            bob   to response(sessionLimit = 1, wednesday to Availability.AVAILABLE, thursday to Availability.AVAILABLE),
            carol to response(sessionLimit = 1, wednesday to Availability.IF_NEED_BE, thursday to Availability.UNAVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        assertEquals(2, plans.size)
        assertEquals(wednesday, plans[0].sessions.single().timeSlot) // 0 missing + 1 if-need-be → first
        assertEquals(thursday,  plans[1].sessions.single().timeSlot) // 1 missing → second
    }

    @Test
    fun `score - plans with non-consecutive sessions rank above consecutive sessions`() {
        val conf = config(
            activity(1, alice to false, bob to false),
            rules = mapOf(
                DayOfWeek.MONDAY   to LocalTime.of(18, 0),
                DayOfWeek.TUESDAY  to LocalTime.of(18, 0),
                DayOfWeek.THURSDAY to LocalTime.of(18, 0),
            ),
        )
        val responses = responses(
            alice to response(monday to Availability.AVAILABLE, tuesday to Availability.AVAILABLE, thursday to Availability.AVAILABLE),
            bob   to response(monday to Availability.AVAILABLE, tuesday to Availability.AVAILABLE, thursday to Availability.AVAILABLE),
        )

        val plans = planner(conf, responses).generatePossiblePlans()

        fun planSlots(p: Plan) = p.sessions.map { it.timeSlot }.toSet()
        val monTuePlan = plans.first { planSlots(it) == setOf(monday, tuesday) }
        val monThuPlan = plans.first { planSlots(it) == setOf(monday, thursday) }
        val tueThuPlan = plans.first { planSlots(it) == setOf(tuesday, thursday) }

        assertEquals(1, monTuePlan.score.directlyFollowingDays)
        assertEquals(0, monThuPlan.score.directlyFollowingDays)
        assertEquals(0, tueThuPlan.score.directlyFollowingDays)

        assertTrue(plans.indexOf(monThuPlan) < plans.indexOf(monTuePlan), "Mon+Thu should outrank Mon+Tue")
        assertTrue(plans.indexOf(tueThuPlan) < plans.indexOf(monTuePlan), "Tue+Thu should outrank Mon+Tue")
    }

    // ─── Score unit tests ─────────────────────────────────────────────────────

    @Test
    fun `Score ordering - missing first, then if-need-be, then more sessions, then more participants, then fewer consecutive days`() {
        val s1 = Plan.Score(missingOptionalParticipants = 0, ifNeedBeParticipants = 0, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 0)
        val s2 = Plan.Score(missingOptionalParticipants = 0, ifNeedBeParticipants = 0, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 1)
        val s3 = Plan.Score(missingOptionalParticipants = 0, ifNeedBeParticipants = 0, numberOfSessions = 2, participantSessions = 4, directlyFollowingDays = 0)
        val s4 = Plan.Score(missingOptionalParticipants = 0, ifNeedBeParticipants = 0, numberOfSessions = 1, participantSessions = 3, directlyFollowingDays = 0)
        val s5 = Plan.Score(missingOptionalParticipants = 0, ifNeedBeParticipants = 1, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 0)
        val s6 = Plan.Score(missingOptionalParticipants = 1, ifNeedBeParticipants = 0, numberOfSessions = 2, participantSessions = 6, directlyFollowingDays = 0)

        assertEquals(listOf(s1, s2, s3, s4, s5, s6), listOf(s6, s4, s2, s5, s3, s1).sorted())
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun planner(
        configuration: Configuration,
        availabilityResponses: Map<DiscordUserId, AvailabilityResponse> = emptyMap(),
    ) = Planner(
        configuration = configuration,
        planningProcess = planningProcess(availabilityResponses)
    )

    private fun planningProcess(availabilityResponses: Map<DiscordUserId, AvailabilityResponse>): PlanningProcess {
        val tenant = Tenant(DiscordServerId(0L), DiscordTextChannelId(0L))
        return PlanningProcess(
            dateRange = DateRange.weekFrom(weekStart),
            tenant = tenant,
            state = PlanningProcess.State.COLLECTING_AVAILABILITIES,
            availabilityInterface = AvailabilityMessage(
                postedAt = Instant.EPOCH,
                message = DiscordMessageId(0L)
            ),
            availabilityResponses = availabilityResponses,
            sentReminders = emptyList(),
            conclusion = null,
            planAlternatives = emptyList(),
        )
    }

    private fun config(
        vararg activities: Activity,
        rules: Map<DayOfWeek, LocalTime> = mapOf(DayOfWeek.MONDAY to LocalTime.of(18, 0)),
    ) = Configuration(
        tenant = Tenant(DiscordServerId(0L), DiscordTextChannelId(0L)),
        localization = Localization(timeZone = UTC, startDayOfWeek = DayOfWeek.MONDAY),
        activities = activities.toList(),
        timeSlotRules = rules,
        reminders = Reminders.DEFAULT,
        periodicPlanning = PeriodicPlanning.DEFAULT,
    )

    private fun activity(id: Int, vararg participants: Pair<Long, Boolean>, maxMissing: Int = 0) =
        Activity(
            id = ActivityId(id),
            name = "Activity $id",
            members = participants.map { (userId, optional) -> Member(DiscordUserId(userId), optional) },
            maxNumMissingOptionalMembers = maxMissing,
            voiceChannel = null,
        )

    private fun responses(vararg pairs: Pair<Long, AvailabilityResponse>): Map<DiscordUserId, AvailabilityResponse> =
        pairs.associate { (userId, response) -> DiscordUserId(userId) to response }

    private fun response(vararg availabilities: Pair<Instant, Availability>) =
        AvailabilityResponse(
            sessionLimit = 2,
            slotAvailabilities = availabilities.toMap(),
        )

    private fun response(sessionLimit: Int, vararg availabilities: Pair<Instant, Availability>) =
        AvailabilityResponse(
            sessionLimit = sessionLimit,
            slotAvailabilities = availabilities.toMap(),
        )
}
