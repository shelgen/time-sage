package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PlanningProcessesScreen(tenant: Tenant) : Screen(tenant) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val processes = PlanningProcessRepository.loadAll(tenant)
            .sortedBy { it.dateRange.fromInclusive }
        if (processes.isEmpty()) {
            return listOf(TextDisplay.of("There are no planning processes."))
        }
        return listOf(TextDisplay.of("## Planning processes")) +
                processes.map { process ->
                    Section.of(
                        Buttons.Manage(process.dateRange, this@PlanningProcessesScreen).render(),
                        TextDisplay.of(process.dateRange.toLocalizedString(configuration.localization))
                    )
                }
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
    }
}
