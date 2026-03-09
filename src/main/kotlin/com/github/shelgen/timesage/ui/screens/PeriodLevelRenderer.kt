package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
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
        toggleSessionLimitButtonFactory: () -> ToggleSessionLimitButton<*>,
        previewAlternativesButtonFactory: () -> PreviewAlternativesButton<*>,
    ): List<MessageTopLevelComponent> {
        val tenant = configuration.tenant
        val dateRangeState = PlanningProcessRepository.load(dateRange, tenant)
            ?: error("Planning process for $dateRange in $tenant does not exist!")
        return renderMissingResponses(dateRangeState, configuration) +
                renderSessionLimits(
                    planningProcess = dateRangeState,
                    globalSessionLimit = configuration.sessionLimit,
                    buttonFactory = toggleSessionLimitButtonFactory
                ) +
                listOf(ActionRow.of(previewAlternativesButtonFactory().render()))
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

    private fun renderSessionLimits(
        planningProcess: PlanningProcess,
        globalSessionLimit: Int,
        buttonFactory: () -> ToggleSessionLimitButton<*>,
    ) = listOf(
        Container.of(
            Section.of(
                buttonFactory().render()
                    .let { if (planningProcess.isLocked()) it.asDisabled() else it },
                TextDisplay.of(
                    "### Limits this period\n" +
                            ((1 until globalSessionLimit).map { limit ->
                                val label = if (limit == 1) "Only one session" else "Only $limit sessions"
                                planningProcess.availabilityResponses
                                    .asSequence()
                                    .filter { (_, response) -> response.sessionLimit == limit }
                                    .map { (userId, _) -> userId }
                                    .sortedBy(DiscordUserId::id)
                                    .toList()
                                    .takeUnless { it.isEmpty() }
                                    ?.joinToString(
                                        prefix = DiscordFormatter.bold(label) + "\n",
                                        transform = DiscordUserId::toMention,
                                        separator = "\n"
                                    ).orEmpty()
                            } + listOf(
                                planningProcess.availabilityResponses
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
                            )).filter { it.isNotEmpty() }
                                .joinToString(separator = "\n\n")
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
                val globalSessionLimit = ConfigurationRepository.loadOrInitialize(screen.tenant).sessionLimit
                val planningProcess = PlanningProcessRepository.load(screen.dateRange, screen.tenant)
                    ?: error("Couldn't find matching planning process for interaction!")
                PlanningProcessRepository.update(planningProcess) { planningProcess ->
                    val old = planningProcess.availabilityResponses[userId]?.sessionLimit ?: globalSessionLimit
                    val new = cycleSessionLimit(old, globalSessionLimit)
                    logger.info("Updating session limit for ${screen.dateRange} from $old to $new")
                    planningProcess.setSessionLimit(userId, new, globalSessionLimit)
                }
            }
        }

        private fun cycleSessionLimit(current: Int, globalSessionLimit: Int): Int =
            (current + globalSessionLimit - 1) % (globalSessionLimit + 1)
    }

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
