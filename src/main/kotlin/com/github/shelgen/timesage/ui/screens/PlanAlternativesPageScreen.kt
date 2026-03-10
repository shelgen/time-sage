package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.createScheduledEventsForPlan
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.plan.PlanId
import com.github.shelgen.timesage.planning.AvailabilityMessageSender
import com.github.shelgen.timesage.planning.Conclusion
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.AlternativePrinter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanAlternativesPageScreen(
    val dateRange: DateRange,
    val fromInclusive: Int,
    val pageSize: Int,
    tenant: Tenant
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, tenant) ?: return planningProcessNotFound()
        val allPlans = planningProcess.planAlternatives
        val numberedPlansInPage =
            allPlans.drop(fromInclusive).take(pageSize)
                .mapIndexed { index, plan -> fromInclusive + index + 1 to plan }
        val header = if (allPlans.isEmpty()) {
            listOf(
                TextDisplay.of("There are no possible alternatives given the current availabilities."),
            )
        } else if (numberedPlansInPage.isEmpty()) {
            listOf(TextDisplay.of("There are no more plans (only ${allPlans.size} in total)."))
        } else {
            renderHeader(numberedPlansInPage, allPlans.size)
        }
        return listOf(
            header,
            renderAlternatives(numberedPlansInPage, configuration),
            renderNavigationActionRow(allPlans.size),
            renderBottomActionRow()
        ).flatten()
    }

    private fun renderHeader(alternativeNumberedPlans: List<Pair<Int, Plan>>, totalNumAlternatives: Int) =
        listOf(
            TextDisplay.of(
                "Alternatives " +
                        alternativeNumberedPlans.first().component1().toString() +
                        " through " +
                        alternativeNumberedPlans.last().component1().toString() +
                        " out of $totalNumAlternatives:"
            )
        )

    private fun renderAlternatives(
        alternativeNumberedPlans: List<Pair<Int, Plan>>,
        configuration: Configuration
    ) =
        alternativeNumberedPlans.map { (alternativeNumber, plan) ->
            renderAlternative(alternativeNumber, plan, configuration)
        }

    private fun renderAlternative(alternativeNumber: Int, plan: Plan, configuration: Configuration) =
        Container.of(
            Section.of(
                Buttons.ConcludeAlternative(
                    planId = plan.id,
                    screen = this@PlanAlternativesPageScreen
                ).render(),
                TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
            )
        )

    private fun renderNavigationActionRow(totalPlans: Int): List<MessageTopLevelComponent> {
        val buttons = buildList<ActionRowChildComponent> {
            if (fromInclusive > 0) add(Buttons.Previous(screen = this@PlanAlternativesPageScreen).render())
            if (fromInclusive + pageSize < totalPlans) add(Buttons.Next(screen = this@PlanAlternativesPageScreen).render())
        }
        return if (buttons.isEmpty()) emptyList() else listOf(ActionRow.of(buttons))
    }

    private fun renderBottomActionRow(): List<MessageTopLevelComponent> {
        val buttons = buildList<ActionRowChildComponent> {
            if (fromInclusive == 0) add(Buttons.ConcludeNoSession(screen = this@PlanAlternativesPageScreen).render())
            add(Buttons.Back(screen = this@PlanAlternativesPageScreen).render())
        }
        return listOf(ActionRow.of(buttons))
    }

    class Buttons {
        class ConcludeAlternative(
            val planId: PlanId,
            override val screen: PlanAlternativesPageScreen
        ) : ScreenButton {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Conclude")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen(
                    onMessagePosted = { conclusionMessage ->
                        val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                        val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                        val plan = getPlan(planId, planningProcess)
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
                    PlanConcludedWithScreen(
                        planId = planId,
                        dateRange = screen.dateRange,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class Previous(override val screen: PlanAlternativesPageScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Previous")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanAlternativesPageScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = maxOf(0, screen.fromInclusive - screen.pageSize),
                        pageSize = screen.pageSize,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class Next(override val screen: PlanAlternativesPageScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Next")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanAlternativesPageScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = screen.fromInclusive + screen.pageSize,
                        pageSize = screen.pageSize,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class ConcludeNoSession(override val screen: PlanAlternativesPageScreen) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "No session")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen(
                    onMessagePosted = { conclusionMessage ->
                        val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)!!
                        val configuration = ConfigurationRepository.loadOrInitialize(screen.tenant)
                        val conclusionMessageId = DiscordMessageId(conclusionMessage.idLong)
                        PlanningProcessRepository.update(planningProcess) { mutable ->
                            mutable.conclusion = Conclusion(
                                message = conclusionMessageId,
                                plan = NO_SESSION_PLAN_ID,
                                scheduledEvents = emptyList(),
                            )
                            mutable.state = PlanningProcess.State.CONCLUDED
                        }
                        AvailabilityMessageSender.rerenderAvailabilityInterface(planningProcess)
                        planningProcess.availabilityInterface?.let { JDAHolder.unpin(it, screen.tenant) }
                        planningProcess.availabilityInterface?.let { JDAHolder.archiveThread(it) }
                        JDAHolder.pin(conclusionMessageId, screen.tenant)
                    }
                ) {
                    PlanConcludedWithScreen(
                        planId = NO_SESSION_PLAN_ID,
                        dateRange = screen.dateRange,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class Back(override val screen: PlanAlternativesPageScreen) : ScreenButton {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanningProcessManageScreen(
                        dateRange = screen.dateRange,
                        tenant = screen.tenant
                    )
                }
            }
        }
    }
}
