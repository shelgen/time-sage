package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.Localization
import com.github.shelgen.timesage.domain.Scheduling
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.domain.TimeSlotRules
import com.github.shelgen.timesage.planning.Plan
import com.github.shelgen.timesage.planning.PlannedSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime.of
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
            enabled = true,
            localization = Localization(timeZone = getTimeZone("UTC"), startDayOfWeek = DayOfWeek.MONDAY),
            scheduling = Scheduling(
                type = SchedulingType.WEEKLY,
                timeSlotRules = TimeSlotRules.of(DayOfWeek.MONDAY to of(18, 0)),
                numDaysInAdvanceToStartPlanning = 5,
                timeOfDayToStartPlanning = 17,
                reminderIntervalDays = 1,
            ),
            activities = listOf(
                Activity(
                    id = 1,
                    name = "Save the World",
                    members = listOf(
                        ActivityMember(cyclops, optional = false),
                        ActivityMember(jeanGrey, optional = false),
                        ActivityMember(wolverine, optional = true),
                    ),
                    maxMissingOptionalMembers = 1,
                ),
                Activity(
                    id = 2,
                    name = "Prevent Mutant Extinction",
                    members = listOf(
                        ActivityMember(storm, optional = false),
                        ActivityMember(rogue, optional = false),
                    ),
                    maxMissingOptionalMembers = 0,
                ),
            ),
            voiceChannelId = null,
        )
    )

    @Test
    fun `single session with all participants available`() {
        val output = printer.printAlternative(
            alternativeNumber = 1,
            plan = Plan(listOf(session(monday, 1, cyclops to false, jeanGrey to false, wolverine to false)))
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
            plan = Plan(listOf(session(monday, 1, cyclops to false, jeanGrey to false, wolverine to true)))
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
            plan = Plan(listOf(session(monday, 1, cyclops to false, jeanGrey to false, missingOptionalCount = 1)))
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
            plan = Plan(listOf(session(monday, 1, cyclops to false, jeanGrey to true, missingOptionalCount = 1)))
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
            plan = Plan(
                listOf(
                    session(monday, 1, cyclops to false, jeanGrey to false, wolverine to false),
                    session(tuesday, 2, storm to false, rogue to false),
                )
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

    private fun session(
        timeSlot: Instant,
        activityId: Int,
        vararg participants: Pair<Long, Boolean>,
        missingOptionalCount: Int = 0,
    ) = PlannedSession(
        timeSlot = timeSlot,
        activityId = activityId,
        participants = participants.map { (userId, ifNeedBe) -> PlannedSession.Participant(userId, ifNeedBe) }.toSet(),
        missingOptionalCount = missingOptionalCount,
    )
}
