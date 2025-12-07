package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.util.*

val mainCategories = setOf(
    "Africa",
    "America",
    "Asia",
    "Atlantic",
    "Australia",
    "Canada",
    "Europe",
    "Indian",
    "Pacific",
    "US",
)

const val CUSTOM_PREFIX = "other"

class ConfigurationTimeZoneMainScreen(context: OperationContext) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        listOf(
            TextDisplay.of(
                "# Time Sage configuration\n" +
                        "## Time zone selection\n" +
                        "### Group selection"
            )
        ) + TimeZone.getAvailableIDs().groupBy { it.substringBefore('/') }
            .entries
            .filter { it.key in mainCategories }
            .sortedBy { it.key }
            .map { (key, value) ->
                Section.of(
                    Buttons.SelectTimeZone(key, this).render(),
                    TextDisplay.of("${DiscordFormatter.bold(key)} (${value.size} zones)")
                )
            } + Section.of(
            Buttons.SelectTimeZone(CUSTOM_PREFIX, this).render(),
            TextDisplay.of(
                "${DiscordFormatter.bold("Others")} (${
                    TimeZone.getAvailableIDs().filterNot { it.substringBefore('/') in mainCategories }.size
                } zones)"
            )
        )

    class Buttons {
        class SelectTimeZone(
            private val prefix: String,
            override val screen: ConfigurationTimeZoneMainScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Select...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationTimeZoneSubScreen(prefix, 0, screen.context) }
            }
        }
    }
}
