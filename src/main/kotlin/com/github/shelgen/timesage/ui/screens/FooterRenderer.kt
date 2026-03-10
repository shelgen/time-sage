package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object FooterRenderer {
    fun renderComponents(previewAlternativesButtonFactory: () -> PreviewAlternativesButton<*>) =
        listOf(ActionRow.of(previewAlternativesButtonFactory().render()))

    abstract class PreviewAlternativesButton<T : AbstractDateRangeScreen>(override val screen: T) : ScreenButton {
        fun render() =
            Button.secondary(CustomIdSerialization.serialize(this), "Preview alternatives")

        override fun handle(event: ButtonInteractionEvent) {
            event.processAndAddEphemeralScreen {
                val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)
                    ?: error("Couldn't find matching planning process for interaction!")
                val planner = Planner(ConfigurationRepository.loadOrInitialize(screen.tenant), planningProcess)
                PreviewAlternativesScreen(
                    dateRange = screen.dateRange,
                    fromInclusive = 0,
                    pageSize = 3,
                    inputHashCode = planner.hashCodeForInput(),
                    tenant = screen.tenant,
                )
            }
        }
    }
}
