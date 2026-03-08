package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Localization
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.configuration.PeriodicPlanning
import com.github.shelgen.timesage.configuration.Reminders
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.plan.Participant
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.plan.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime.of
import java.util.*
import java.util.TimeZone.getTimeZone

class AlternativePrinterTest {
    private val cyclops = 1L
    private val jeanGrey = 2L
    private val wolverine = 3L
    private val storm = 4L
    private val rogue = 5L

    private val monday = Instant.parse("2025-08-04T18:00:00Z")
    private val tuesday = Instant.parse("2025-08-05T18:00:00Z")

    private val printer = AlternativePrinter(
        Configuration(
            tenant = Tenant(DiscordServerId(0), DiscordTextChannelId(0)),
            localization = Localization(timeZone = getTimeZone("UTC"), startDayOfWeek = DayOfWeek.MONDAY),
            activities = listOf(
                Activity(
                    id = ActivityId(1),
                    name = "Save the World",
                    members = listOf(
                        Member(DiscordUserId(cyclops), optional = false),
                        Member(DiscordUserId(jeanGrey), optional = false),
                        Member(DiscordUserId(wolverine), optional = true),
                    ),
                    maxNumMissingOptionalMembers = 1,
                    voiceChannel = null,
                ),
                Activity(
                    id = ActivityId(2),
                    name = "Prevent Mutant Extinction",
                    members = listOf(
                        Member(DiscordUserId(storm), optional = false),
                        Member(DiscordUserId(rogue), optional = false),
                    ),
                    maxNumMissingOptionalMembers = 0,
                    voiceChannel = null,
                ),
            ),
            timeSlotRules = mapOf(DayOfWeek.MONDAY to of(18, 0)),
            reminders = Reminders.DEFAULT,
            periodicPlanning = PeriodicPlanning.DEFAULT,
            sessionLimit = 2,
        )
    )

    @Test
    fun `single session with all participants available`() {
        val output = printer.printAlternative(
            alternativeNumber = 1,
            plan = plan(session(monday, 1, cyclops to false, jeanGrey to false, wolverine to false))
        )
        assertEquals(
            "-# Alternative 1 (1 session, 3 participants)\n" +
                    "### **Save the World** on <t:${monday.epochSecond}:F>\n" +
                    "<@$cyclops>\n" +
                    "<@$jeanGrey>\n" +
                    "<@$wolverine>\n",
            output
        )
    }

    @Test
    fun `if-need-be attendee shown in italics`() {
        val output = printer.printAlternative(
            alternativeNumber = 1,
            plan = plan(session(monday, 1, cyclops to false, jeanGrey to false, wolverine to true))
        )
        assertEquals(
            "-# Alternative 1 (1 if-need-be, 1 session, 3 participants)\n" +
                    "### **Save the World** on <t:${monday.epochSecond}:F>\n" +
                    "<@$cyclops>\n" +
                    "<@$jeanGrey>\n" +
                    "*<@$wolverine> (if need be)*\n",
            output
        )
    }

    @Test
    fun `absent optional participant shown as strikethrough with score showing missing`() {
        val output = printer.printAlternative(
            alternativeNumber = 1,
            plan = plan(session(monday, 1, cyclops to false, jeanGrey to false, missingOptionalCount = 1))
        )
        assertEquals(
            "-# Alternative 1 (1 missing, 1 session, 2 participants)\n" +
                    "### **Save the World** on <t:${monday.epochSecond}:F>\n" +
                    "<@$cyclops>\n" +
                    "<@$jeanGrey>\n" +
                    "~~<@$wolverine>~~ (unavailable)\n",
            output
        )
    }

    @Test
    fun `all three attendance states in one session`() {
        val output = printer.printAlternative(
            alternativeNumber = 2,
            plan = plan(session(monday, 1, cyclops to false, jeanGrey to true, missingOptionalCount = 1))
        )
        assertEquals(
            "-# Alternative 2 (1 missing, 1 if-need-be, 1 session, 2 participants)\n" +
                    "### **Save the World** on <t:${monday.epochSecond}:F>\n" +
                    "<@$cyclops>\n" +
                    "*<@$jeanGrey> (if need be)*\n" +
                    "~~<@$wolverine>~~ (unavailable)\n",
            output
        )
    }

    @Test
    fun `two sessions on consecutive days includes directly following day in score`() {
        val output = printer.printAlternative(
            alternativeNumber = 1,
            plan = plan(
                session(monday, 1, cyclops to false, jeanGrey to false, wolverine to false),
                session(tuesday, 2, storm to false, rogue to false),
            )
        )
        assertEquals(
            "-# Alternative 1 (2 sessions, 5 participants, 1 directly following day)\n" +
                    "### **Save the World** on <t:${monday.epochSecond}:F>\n" +
                    "<@$cyclops>\n" +
                    "<@$jeanGrey>\n" +
                    "<@$wolverine>\n" +
                    "\n" +
                    "### **Prevent Mutant Extinction** on <t:${tuesday.epochSecond}:F>\n" +
                    "<@$storm>\n" +
                    "<@$rogue>\n",
            output
        )
    }

    private fun plan(vararg sessions: Session) = Plan(
        id = PlanId(UUID.randomUUID()),
        sessions = sessions.toList()
    )

    private fun session(
        timeSlot: Instant,
        activityId: Int,
        vararg participants: Pair<Long, Boolean>,
        missingOptionalCount: Int = 0,
    ) = Session(
        timeSlot = timeSlot,
        activityId = ActivityId(activityId),
        participants = participants.map { (userId, ifNeedBe) -> Participant(DiscordUserId(userId), ifNeedBe) }.toSet(),
        missingOptionalCount = missingOptionalCount,
    )
}
