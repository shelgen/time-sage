package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object PeriodLevelRenderer {
    fun renderComponents(
        dateRange: DateRange,
        configuration: Configuration,
        buttonFactory: () -> ToggleSessionLimitButton<*>,
    ): List<MessageTopLevelComponent> {
        val tenant = configuration.tenant
        val dateRangeState = PlanningProcessRepository.load(dateRange, tenant)
            ?: error("Planning process for $dateRange in $tenant does not exist!")
        return renderMissingResponses(dateRangeState, configuration) +
                renderLimits(
                    title = "Limits this ${dateRange.toLocalizedString(configuration.localization)}",
                    data = dateRangeState,
                    buttonFactory = buttonFactory
                )
    }

    private fun renderMissingResponses(data: PlanningProcess, configuration: Configuration) =
        listOfNotNull(
            configuration.activities
                .flatMap(Activity::members)
                .map(Member::user)
                .filter { data.availabilityResponses[it] == null }
                .distinct()
                .sortedBy(DiscordUserId::id)
                .takeUnless(List<DiscordUserId>::isEmpty)
                ?.joinToString(
                    prefix = "### Missing responses\n",
                    separator = "\n"
                ) { userId -> userId.toMention() }
                ?.let(TextDisplay::of)
                ?.let { Container.of(it).withAccentColor(0xFFB6C1) }
        )

    private fun renderLimits(
        title: String,
        data: PlanningProcess,
        buttonFactory: () -> ToggleSessionLimitButton<*>,
    ) = listOf(
        Container.of(
            Section.of(
                buttonFactory().render()
                    .let { if (data.state == PlanningProcess.State.CONCLUDED) it.asDisabled() else it },
                TextDisplay.of(
                    "### $title\n" +
                            listOf(
                                data.availabilityResponses
                                    .asSequence()
                                    .filter { (_, response) -> response.sessionLimit == 1 }
                                    .map { (userId, _) -> userId }
                                    .sortedBy(DiscordUserId::id)
                                    .toList()
                                    .takeUnless { it.isEmpty() }
                                    ?.joinToString(
                                        prefix = DiscordFormatter.bold("Only one session") + "\n",
                                        transform = DiscordUserId::toMention,
                                        separator = "\n"
                                    ).orEmpty(),
                                data.availabilityResponses
                                    .asSequence()
                                    .filter { (_, response) -> response.sessionLimit == 0 }
                                    .map { (userId, _) -> userId }
                                    .sortedBy(DiscordUserId::id)
                                    .toList()
                                    .takeUnless { it.isEmpty() }
                                    ?.joinToString(
                                        prefix = DiscordFormatter.bold("Can't make it at all") + "\n",
                                        transform = DiscordUserId::toMention,
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
                val userId = DiscordUserId(event.user.idLong)
                val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)
                    ?: error("Couldn't find matching planning process for interaction!")
                PlanningProcessRepository.update(planningProcess) { period ->
                    val old = period.availabilityResponses[userId]?.sessionLimit
                    val new = cycleLimit(old)
                    logger.info("Updating session limit for target period ${screen.dateRange} from $old to $new")
                    period.setSessionLimit(userId, new)
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
