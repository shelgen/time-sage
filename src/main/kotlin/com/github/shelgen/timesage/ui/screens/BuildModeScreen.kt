package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.createScheduledEventsForPlan
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.Session
import com.github.shelgen.timesage.planning.AvailabilityMessageSender
import com.github.shelgen.timesage.planning.Conclusion
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.time.TimeSlot
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

/**
 * Screen for building a plan session-by-session.
 *
 * [selectedSessions] encodes the sessions the user has selected so far as
 * "<slotIdx>:<actId>" tokens joined by ";", e.g. "0:1;2:1". This stays
 * compact enough to remain well under the 100-character custom-ID limit.
 */
class BuildModeScreen(
    val dateRange: DateRange,
    val selectedSessions: String,
    tenant: Tenant,
) : Screen(tenant) {

    fun parseSelectedSessions(): List<Pair<Int, Int>> =
        if (selectedSessions.isEmpty()) emptyList()
        else selectedSessions.split(';').map { token ->
            val (slotIdxStr, actIdStr) = token.split(':', limit = 2)
            slotIdxStr.toInt() to actIdStr.toInt()
        }

    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, tenant)
            ?: return planningProcessNotFound()

        val parsed = parseSelectedSessions()
        val matchingPlans = planningProcess.planAlternatives
            .filter { plansMatchSelections(it, parsed, planningProcess.timeSlots) }

        val components = mutableListOf<MessageTopLevelComponent>()

        components.add(
            TextDisplay.of(
                "## Build your plan\n" +
                        "Select sessions one by one to construct your plan."
            )
        )

        val bestPlan = matchingPlans.firstOrNull()
        if (parsed.isNotEmpty()) {
            if (bestPlan == null) {
                components.add(
                    TextDisplay.of("⚠️ No plans match your current selections. Please go back and start over.")
                )
            } else {
                for ((slotIdx, actId) in parsed) {
                    val timeSlot = planningProcess.timeSlots[slotIdx]
                    val session = bestPlan.sessions.first {
                        it.timeSlot == timeSlot && it.activityId.value == actId
                    }
                    components.add(renderSelectedSession(session, configuration))
                }
            }
        }

        val nextSessions = if (matchingPlans.isNotEmpty()) {
            getNextSessionOptions(matchingPlans, parsed, planningProcess.timeSlots)
        } else {
            emptyList()
        }

        if (nextSessions.isNotEmpty()) {
            val options = nextSessions.take(25).map { (slotIdx, actId) ->
                val timeSlot = planningProcess.timeSlots[slotIdx]
                val activity = configuration.getActivity(ActivityId(actId))
                val label = "${activity.name}: ${DiscordFormatter.timestamp(timeSlot, DiscordFormatter.TimestampFormat.SHORT_DATE_TIME)}"
                SelectOption.of(label.take(100), "$slotIdx:$actId")
            }
            components.add(
                ActionRow.of(SelectMenus.SelectNextSession(this).render(options))
            )
        } else if (parsed.isNotEmpty() && matchingPlans.isNotEmpty()) {
            components.add(TextDisplay.of("-# All sessions for matching plans are selected."))
        }

        val bottomButtons = buildList<ActionRowChildComponent> {
            if (parsed.isNotEmpty() && matchingPlans.isNotEmpty()) {
                add(Buttons.ConcludeBestMatch(this@BuildModeScreen).render())
            }
            add(Buttons.Back(this@BuildModeScreen).render())
        }
        components.add(ActionRow.of(bottomButtons))

        return components
    }

    private fun renderSelectedSession(session: Session, configuration: Configuration): MessageTopLevelComponent {
        val activity = configuration.getActivity(session.activityId)
        val timeStr = DiscordFormatter.timestamp(session.timeSlot, DiscordFormatter.TimestampFormat.LONG_DATE_TIME)
        val participantLines = activity.members
            .sortedBy { it.user.id }
            .joinToString("\n") { member ->
                val participant = session.participants.firstOrNull { it.user == member.user }
                when {
                    participant == null ->
                        DiscordFormatter.strikethrough(member.user.toMention()) + " (unavailable)"
                    participant.ifNeedBe ->
                        DiscordFormatter.italics(member.user.toMention() + " (if need be)")
                    else ->
                        member.user.toMention()
                }
            }
        return Container.of(
            TextDisplay.of("### ✅ ${DiscordFormatter.bold(activity.name)} on $timeStr\n$participantLines")
        )
    }

    private fun getNextSessionOptions(
        matchingPlans: List<Plan>,
        selected: List<Pair<Int, Int>>,
        timeSlots: List<TimeSlot>,
    ): List<Pair<Int, Int>> =
        matchingPlans
            .flatMap { plan -> plan.sessions }
            .map { session -> timeSlots.indexOf(session.timeSlot) to session.activityId.value }
            .filter { it !in selected }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))

    class SelectMenus {
        class SelectNextSession(override val screen: BuildModeScreen) : ScreenStringSelectMenu {
            fun render(options: List<SelectOption>): StringSelectMenu =
                StringSelectMenu.create(CustomIdSerialization.serialize(this))
                    .setPlaceholder("Pick next session...")
                    .addOptions(options)
                    .build()

            override fun handle(event: StringSelectInteractionEvent) {
                val value = event.selectedOptions.first().value
                val newSelected = if (screen.selectedSessions.isEmpty()) value
                else "${screen.selectedSessions};$value"
                event.processAndNavigateTo {
                    BuildModeScreen(
                        dateRange = screen.dateRange,
                        selectedSessions = newSelected,
                        tenant = screen.tenant,
                    )
                }
            }
        }
    }

    class Buttons {
        class ConcludeBestMatch(override val screen: BuildModeScreen) : ScreenButton {
            fun render(): Button =
                Button.success(CustomIdSerialization.serialize(this), "Conclude with best match")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen(
                    onMessagePosted = { conclusionMessage ->
                        val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                        val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                        val parsed = screen.parseSelectedSessions()
                        val plan = planningProcess.planAlternatives.first {
                            plansMatchSelections(it, parsed, planningProcess.timeSlots)
                        }
                        val conclusionMessageId = DiscordMessageId(conclusionMessage.idLong)
                        PlanningProcessRepository.update(planningProcess) { mutable ->
                            mutable.conclusion = Conclusion(
                                message = conclusionMessageId,
                                plan = plan.id,
                                scheduledEvents = emptyList(),
                            )
                            mutable.state = PlanningProcess.State.CONCLUDED
                        }
                        AvailabilityMessageSender.rerenderAvailabilityInterface(planningProcess)
                        planningProcess.availabilityInterface?.let { JDAHolder.unpin(it, screen.tenant) }
                        planningProcess.availabilityInterface?.let { JDAHolder.archiveThread(it) }
                        JDAHolder.pin(conclusionMessageId, screen.tenant)
                        if (plan.sessions.isNotEmpty()) {
                            JDAHolder.jda.getGuildById(screen.tenant.server.id)?.let { guild ->
                                createScheduledEventsForPlan(plan, guild, configuration) { scheduledEventIds ->
                                    val updated = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                                    PlanningProcessRepository.update(updated) { mutable ->
                                        mutable.conclusion =
                                            mutable.conclusion?.copy(scheduledEvents = scheduledEventIds)
                                    }
                                }
                            }
                        }
                    }
                ) {
                    val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                    val parsed = screen.parseSelectedSessions()
                    val plan = planningProcess.planAlternatives.first {
                        plansMatchSelections(it, parsed, planningProcess.timeSlots)
                    }
                    PlanConcludedWithScreen(
                        planId = plan.id,
                        dateRange = screen.dateRange,
                        tenant = screen.tenant,
                    )
                }
            }
        }

        class Back(override val screen: BuildModeScreen) : ScreenButton {
            fun render(): Button =
                Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanAlternativesPageScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = 0,
                        pageSize = 3,
                        tenant = screen.tenant,
                    )
                }
            }
        }
    }

    companion object {
        fun plansMatchSelections(
            plan: Plan,
            selected: List<Pair<Int, Int>>,
            timeSlots: List<TimeSlot>,
        ): Boolean =
            selected.all { (slotIdx, actId) ->
                plan.sessions.any { session ->
                    timeSlots.indexOf(session.timeSlot) == slotIdx &&
                            session.activityId.value == actId
                }
            }
    }
}
