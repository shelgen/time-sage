package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.PlanningTargetPeriod
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PeriodLevelScreenContent<T : AbstractDateRangeScreen>(
    val screen: T,
    val buttonFactory: () -> ToggleSessionLimitButton<T>
) {
    private val dateRange = screen.dateRange
    private val tenant = screen.tenant

    fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val dateRangeState = AvailabilitiesPeriodRepository.loadOrInitialize(dateRange, tenant)
        return renderMissingResponses(dateRangeState, configuration) +
                renderLimits("Limits this ${dateRange.toLocalizedString(configuration.localization)}", dateRangeState)
    }

    private fun renderMissingResponses(data: PlanningTargetPeriod, configuration: Configuration) =
        listOfNotNull(
            configuration.activities
                .flatMap(Activity::members)
                .map(ActivityMember::userId)
                .filter { data.availabilityResponses[it] == null }
                .distinct()
                .sorted()
                .takeUnless(List<Long>::isEmpty)
                ?.joinToString(
                    prefix = "### Missing responses\n",
                    separator = "\n"
                ) { userId -> DiscordFormatter.mentionUser(userId) }
                ?.let(TextDisplay::of)
                ?.let { Container.of(it).withAccentColor(0xFFB6C1) }
        )

    private fun renderLimits(title: String, data: PlanningTargetPeriod) =
        listOf(
            Container.of(
                Section.of(
                    buttonFactory().render()
                        .let { if (data.concluded) it.asDisabled() else it },
                    TextDisplay.of(
                        "### $title\n" +
                                listOf(
                                    data.availabilityResponses.map
                                        .asSequence()
                                        .mapNotNull { (userId, response) -> response.sessionLimit?.let { userId to it } }
                                        .filter { (_, sessionLimit) -> sessionLimit == 1 }
                                        .map { (userId, _) -> userId }
                                        .sorted()
                                        .toList()
                                        .takeUnless { it.isEmpty() }
                                        ?.joinToString(
                                            prefix = DiscordFormatter.bold("Only one session") + "\n",
                                            transform = DiscordFormatter::mentionUser,
                                            separator = "\n"
                                        ).orEmpty(),
                                    data.availabilityResponses.map
                                        .asSequence()
                                        .mapNotNull { (userId, response) -> response.sessionLimit?.let { userId to it } }
                                        .filter { (_, sessionLimit) -> sessionLimit == 0 }
                                        .map { (userId, _) -> userId }
                                        .sorted()
                                        .toList()
                                        .takeUnless { it.isEmpty() }
                                        ?.joinToString(
                                            prefix = DiscordFormatter.bold("Can't make it at all") + "\n",
                                            transform = DiscordFormatter::mentionUser,
                                            separator = "\n"
                                        ).orEmpty()
                                ).joinToString(separator = "\n\n")
                    )
                )
            )
        )

    abstract class ToggleSessionLimitButton<T : AbstractDateRangeScreen>(override val screen: T) : ScreenButton {
        fun render() =
            Button.secondary(CustomIdSerialization.serialize(this), Emoji.fromUnicode("U+1F6AB"))

        override fun handle(event: ButtonInteractionEvent) {
            event.processAndRerender {
                val userId = event.user.idLong
                AvailabilitiesPeriodRepository.update(screen.dateRange, screen.tenant) { period ->
                    val old = period.availabilityResponses[userId]?.sessionLimit
                    val new = cycleLimit(old)
                    logger.info("Updating session limit for date range ${screen.dateRange} from $old to $new")
                    period.setUserSessionLimit(userId, new)
                }
            }
        }

        private fun cycleLimit(current: Int?) = when (current) {
            0 -> 2
            1 -> 0
            else -> 1
        }
    }
}
