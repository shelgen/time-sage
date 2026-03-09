package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.planning.AvailabilityMessageSender
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanningProcessesScreen(tenant: Tenant) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val planningProcesses = PlanningProcessRepository.loadAll(tenant)
            .sortedBy { it.dateRange.fromInclusive }
        val existingRanges = planningProcesses.map { it.dateRange }.toSet()

        val initiateButtons = buildList {
            val localization = configuration.localization
            val candidates = listOf(
                "This week" to localization.currentWeek(),
                "Next week" to localization.nextWeek(),
                "This month" to localization.currentMonth(),
                "Next month" to localization.nextMonth(),
            )
            candidates
                .filterNot { (_, range) -> range in existingRanges }
                .forEach { (label, range) ->
                    add(Buttons.Initiate(range, label, this@PlanningProcessesScreen).render())
                }
        }

        val existingSection = if (planningProcesses.isEmpty()) {
            listOf(TextDisplay.of("There are no planning processes."))
        } else {
            listOf(TextDisplay.of("## Planning processes")) +
                    planningProcesses.map { process ->
                        Section.of(
                            Buttons.Manage(process.dateRange, this@PlanningProcessesScreen).render(),
                            TextDisplay.of(
                                "- " + process.dateRange.toLocalizedString(configuration.localization)
                                    .capitalize() + " (${process.state.name})"
                            )
                        )
                    }
        }

        val initiateSection = if (initiateButtons.isNotEmpty()) {
            listOf(
                TextDisplay.of("## Initiate planning process for"),
                ActionRow.of(initiateButtons),
            )
        } else {
            emptyList()
        }

        return existingSection + initiateSection
    }

    class Buttons {
        class Manage(
            val dateRange: DateRange,
            override val screen: PlanningProcessesScreen,
        ) : ScreenButton {
            fun render() = Button.primary(CustomIdSerialization.serialize(this), "Manage...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    PlanningProcessManageScreen(dateRange, screen.tenant)
                }
            }
        }

        class Initiate(
            val dateRange: DateRange,
            val label: String,
            override val screen: PlanningProcessesScreen,
        ) : ScreenButton {
            fun render() = Button.primary(CustomIdSerialization.serialize(this), label)

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    AvailabilityMessageSender.postAvailabilityInterface(dateRange, screen.tenant)
                }
            }
        }
    }
}
