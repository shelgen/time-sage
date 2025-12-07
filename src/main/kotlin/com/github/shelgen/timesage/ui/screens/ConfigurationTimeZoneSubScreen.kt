package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.util.*

private const val PAGE_SIZE = 10

class ConfigurationTimeZoneSubScreen(
    private val prefix: String,
    private val startIndex: Int,
    context: OperationContext
) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val timeZoneList =
            if (prefix == CUSTOM_PREFIX) {
                TimeZone.getAvailableIDs()
                    .filterNot { it.substringBefore('/') in mainCategories }
                    .sorted()
            } else {
                TimeZone.getAvailableIDs()
                    .filter { it.substringBefore('/') == prefix }
                    .sorted()
            }

        val page = timeZoneList
            .drop(startIndex)
            .take(PAGE_SIZE)

        return buildList {
            add(
                TextDisplay.of(
                    "# Time Sage configuration\n" +
                            "## Time zone selection\n" +
                            "### Zone selection\n" +
                            "-# Showing $prefix zones ${startIndex + 1} through ${startIndex + page.size} out of ${timeZoneList.size}"
                )
            )
            addAll(
                page
                    .map { zoneId ->
                        Section.of(
                            Buttons.Select(zoneId, this@ConfigurationTimeZoneSubScreen).render(),
                            TextDisplay.of("$zoneId")
                        )
                    }
            )
            val pageButtons = buildList<ActionRowChildComponent> {
                if (startIndex > 0) {
                    add(Buttons.Previous(this@ConfigurationTimeZoneSubScreen).render())
                }
                if (startIndex + PAGE_SIZE < timeZoneList.size) {
                    add(Buttons.Next(this@ConfigurationTimeZoneSubScreen).render())
                }
            }
            if (pageButtons.isNotEmpty()) {
                add(ActionRow.of(pageButtons))
            }
            add(ActionRow.of(Buttons.Back(this@ConfigurationTimeZoneSubScreen).render()))
        }
    }

    class Buttons {
        class Select(
            private val zoneId: String,
            override val screen: ConfigurationTimeZoneSubScreen
        ) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Select")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    ConfigurationRepository.update(screen.context) { configuration ->
                        configuration.timeZone = TimeZone.getTimeZone(zoneId)
                    }
                    ConfigurationMainScreen(screen.context)
                }
            }
        }

        class Previous(override val screen: ConfigurationTimeZoneSubScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Previous")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    ConfigurationTimeZoneSubScreen(
                        prefix = screen.prefix,
                        startIndex = screen.startIndex - PAGE_SIZE,
                        context = screen.context
                    )
                }
            }
        }

        class Next(override val screen: ConfigurationTimeZoneSubScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Next")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    ConfigurationTimeZoneSubScreen(
                        prefix = screen.prefix,
                        startIndex = screen.startIndex + PAGE_SIZE,
                        context = screen.context
                    )
                }
            }
        }

        class Back(override val screen: ConfigurationTimeZoneSubScreen) : ScreenButton {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationMainScreen(screen.context) }
            }
        }
    }
}
