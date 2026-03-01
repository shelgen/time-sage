package com.github.shelgen.timesage.ui

import com.github.shelgen.timesage.domain.*
import com.github.shelgen.timesage.planning.Plan
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
            timeZone = getTimeZone("UTC"),
            scheduling = Scheduling(
                type = SchedulingType.WEEKLY,
                startDayOfWeek = DayOfWeek.MONDAY,
                timeSlotRules = listOf(TimeSlotRule(DayType.MONDAYS, of(18, 0))),
                daysBeforePeriod = 5,
                planningStartHour = 17,
                reminderIntervalDays = 1,
            ),
            activities = listOf(
                Activity(
                    id = 1,
                    name = "Save the World",
                    participants = listOf(
                        Participant(cyclops, optional = false),
                        Participant(jeanGrey, optional = false),
                        Participant(wolverine, optional = true),
                    ),
                    maxMissingOptionalParticipants = 1,
                ),
                Activity(
                    id = 2,
                    name = "Prevent Mutant Extinction",
                    participants = listOf(
                        Participant(storm, optional = false),
                        Participant(rogue, optional = false),
                    ),
                    maxMissingOptionalParticipants = 0,
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
        vararg attendees: Pair<Long, Boolean>,
        missingOptionalCount: Int = 0,
    ) = Plan.Session(
        timeSlot = timeSlot,
        activityId = activityId,
        attendees = attendees.map { (userId, ifNeedBe) -> Plan.Session.Attendee(userId, ifNeedBe) }.toSet(),
        missingOptionalCount = missingOptionalCount,
    )
}
