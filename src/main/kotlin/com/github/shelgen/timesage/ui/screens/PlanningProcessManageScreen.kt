package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.AvailabilityMessage
import com.github.shelgen.timesage.planning.AvailabilityMessageSender
import com.github.shelgen.timesage.planning.AvailabilityThread
import com.github.shelgen.timesage.planning.Conclusion
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.modals.Modal

class PlanningProcessManageScreen(
    val dateRange: DateRange,
    tenant: Tenant,
) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcess = PlanningProcessRepository.load(dateRange, tenant)
            ?: return planningProcessNotFound()

        return listOf(
            TextDisplay.of(
                "## Planning of ${dateRange.toLocalizedString(configuration.localization)}\n" +
                        planningProcess.availabilityInterface?.toLink(tenant).orEmpty() + "\n" +
                        showStatus(planningProcess) + "\n" +
                        planningProcess.conclusion?.linkToMessage(tenant).orEmpty() + "\n" +
                        (planningProcess.sentReminders.lastOrNull()?.let {
                            "Last reminder sent: ${configuration.localization.dateOf(it.sentAt)}"
                        } ?: "No reminders sent yet.")
            ),
        ) + when (planningProcess.state) {
            PlanningProcess.State.PENDING -> listOf(
                ActionRow.of(Buttons.Delete(this).render())
            )

            PlanningProcess.State.COLLECTING_AVAILABILITIES ->
                listOf(ActionRow.of(Buttons.Lock(this).render(), Buttons.Delete(this).render()))

            PlanningProcess.State.LOCKED ->
                listOf(
                    ActionRow.of(
                        Buttons.PickPlan(this).render(),
                        Buttons.Unlock(this).render(),
                        Buttons.Delete(this).render()
                    )
                )

            PlanningProcess.State.CONCLUDED ->
                listOf(ActionRow.of(Buttons.UndoConclusion(this).render(), Buttons.Delete(this).render()))
        } + listOf(ActionRow.of(Buttons.Back(this).render()))
    }

    private fun showStatus(planningProcess: PlanningProcess): String =
        "Currently ${DiscordFormatter.bold(planningProcess.state.toDisplayString())}"

    private fun PlanningProcess.State.toDisplayString() = when (this) {
        PlanningProcess.State.PENDING -> "Just started, still creating messages...."
        PlanningProcess.State.COLLECTING_AVAILABILITIES -> "Actively collecting availabilities"
        PlanningProcess.State.LOCKED -> "Locked and ready for planning"
        PlanningProcess.State.CONCLUDED -> "Concluded:"
    }

    class Buttons {
        class Lock(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.primary(CustomIdSerialization.serialize(this), "Lock and generate plans")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val tenant = screen.tenant
                    val dateRange = screen.dateRange
                    val configuration = ConfigurationRepository.loadOrInitialize(tenant)
                    val planningProcess = PlanningProcessRepository.load(dateRange, tenant)!!
                    val plans = Planner(configuration, planningProcess).generatePossiblePlans()
                    PlanningProcessRepository.update(planningProcess) { planningProcess ->
                        planningProcess.state = PlanningProcess.State.LOCKED
                        planningProcess.planAlternatives = plans
                    }
                    AvailabilityMessageSender.rerenderAvailabilityInterface(planningProcess)
                }
            }
        }

        class PickPlan(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.primary(CustomIdSerialization.serialize(this), "Pick plan...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanAlternativesPageScreen(
                        dateRange = screen.dateRange,
                        fromInclusive = 0,
                        pageSize = 3,
                        tenant = screen.tenant
                    )
                }
            }
        }

        class Unlock(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.secondary(CustomIdSerialization.serialize(this), "Unlock")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val tenant = screen.tenant
                    val dateRange = screen.dateRange
                    val planningProcess = PlanningProcessRepository.load(dateRange, tenant)!!
                    PlanningProcessRepository.update(planningProcess) { mutable ->
                        mutable.state = PlanningProcess.State.COLLECTING_AVAILABILITIES
                        mutable.planAlternatives = mutableListOf()
                    }
                    AvailabilityMessageSender.rerenderAvailabilityInterface(planningProcess)
                }
            }
        }

        class UndoConclusion(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.secondary(CustomIdSerialization.serialize(this), "Undo conclusion")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    val tenant = screen.tenant
                    val dateRange = screen.dateRange
                    val planningProcess = PlanningProcessRepository.load(dateRange, tenant)!!
                    val conclusion = planningProcess.conclusion
                    PlanningProcessRepository.update(planningProcess) { mutable ->
                        mutable.state = PlanningProcess.State.LOCKED
                        mutable.conclusion = null
                    }
                    AvailabilityMessageSender.rerenderAvailabilityInterface(planningProcess)
                    planningProcess.availabilityInterface?.let { JDAHolder.pin(it, tenant) }
                    planningProcess.availabilityInterface?.let { JDAHolder.unarchiveThread(it) }
                    if (conclusion != null) {
                        deleteConclusionArtifacts(conclusion, tenant)
                    }
                }
            }
        }

        class Delete(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.danger(CustomIdSerialization.serialize(this), "Delete")

            override fun handle(event: ButtonInteractionEvent) {
                event.replyModal(Modals.ConfirmDelete(screen).render()).queue()
            }
        }

        class Back(override val screen: PlanningProcessManageScreen) : ScreenButton {
            fun render() = Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { PlanningProcessesScreen(screen.tenant) }
            }
        }
    }

    class Modals {
        class ConfirmDelete(override val screen: PlanningProcessManageScreen) : ScreenModal {
            fun render() = Modal
                .create(CustomIdSerialization.serialize(this), "Delete planning process")
                .addComponents(
                    Label.of(
                        "Are you sure? Responses will also be deleted.",
                        StringSelectMenu.create("confirm")
                            .setMinValues(1)
                            .setMaxValues(1)
                            .addOptions(
                                SelectOption.of("No", "no"),
                                SelectOption.of("Yes", "yes"),
                            )
                            .setDefaultValues("no")
                            .build()
                    )
                )
                .build()

            override fun handle(event: ModalInteractionEvent) {
                val confirmed = event.getValue("confirm")!!.asStringList.first() == "yes"
                if (!confirmed) {
                    event.processAndRerender { }
                    return
                }
                event.processAndNavigateTo {
                    val tenant = screen.tenant
                    val dateRange = screen.dateRange
                    val planningProcess = PlanningProcessRepository.load(dateRange, tenant)!!
                    val textChannel = JDAHolder.getTextChannel(tenant)
                    planningProcess.sentReminders.forEach { reminder ->
                        textChannel.deleteMessageById(reminder.message.id).queue()
                    }
                    planningProcess.conclusion?.let { conclusion ->
                        deleteConclusionArtifacts(conclusion, tenant)
                    }
                    when (val ai = planningProcess.availabilityInterface) {
                        null -> {}
                        is AvailabilityMessage -> textChannel.deleteMessageById(ai.message.id).queue()
                        is AvailabilityThread -> {
                            JDAHolder.getThreadChannel(ai.threadChannel).delete().queue()
                            textChannel.deleteMessageById(ai.threadStartMessage.id).queue()
                        }
                    }
                    PlanningProcessRepository.delete(planningProcess)
                    PlanningProcessesScreen(tenant)
                }
            }
        }
    }

    companion object {
        private fun deleteConclusionArtifacts(
            conclusion: Conclusion,
            tenant: Tenant
        ) {
            val textChannel = JDAHolder.getTextChannel(tenant)
            textChannel.deleteMessageById(conclusion.message.id).queue()
            val guild = JDAHolder.getGuild(tenant)
            conclusion.scheduledEvents.forEach { eventId ->
                guild.retrieveScheduledEventById(eventId.id).queue({ it.delete().queue() }, {})
            }
        }
    }
}
