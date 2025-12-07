package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ConfigurationDeleteActivitiesScreen(context: OperationContext) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        listOf(
            TextDisplay.of(
                "# Time Sage configuration\n" +
                        "## Delete activities"
            ),
        ) + if (configuration.activities.isEmpty()) {
            listOf(TextDisplay.of(DiscordFormatter.italics("There are currently no activities.")))
        } else {
            configuration.activities.map {
                Section.of(
                    Buttons.DeleteActivity(it.id, this).render(),
                    TextDisplay.of("- ${it.name}")
                )
            }
        } + ActionRow.of(
            Buttons.Back(this).render()
        )

    class Buttons {
        class DeleteActivity(
            private val activityId: Int,
            override val screen: ConfigurationDeleteActivitiesScreen
        ) : ScreenButton {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "Delete")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.context) { configuration ->
                        configuration.activities.removeIf { it.id == activityId }
                    }
                }
            }
        }

        class Back(override val screen: ConfigurationDeleteActivitiesScreen) : ScreenButton {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationMainScreen(screen.context) }
            }
        }
    }
}
