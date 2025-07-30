package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.planning.Planner
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.AlternativePrinter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.time.LocalDate

class PlanAlternativeListScreen(
    val weekMondayDate: LocalDate,
    val startIndex: Int,
    val size: Int,
    guildId: Long
) : Screen(guildId) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> {
        val week = WeekRepository.loadOrInitialize(guildId = guildId, mondayDate = weekMondayDate)
        val planner = Planner(configuration = configuration, week = week)
        val plans = planner.generatePossiblePlans()
        val alternativeNumberedPlans =
            plans.drop(startIndex).take(size)
                .mapIndexed { index, weekPlan -> startIndex + index + 1 to weekPlan }
        val totalNumAlternatives = plans.size

        return if (alternativeNumberedPlans.isEmpty()) {
            listOf(TextDisplay.of("There are no possible alternatives given the current availabilities."))
        } else {
            val nextStartIndex = startIndex + size
            val nextSize = size.coerceAtMost(totalNumAlternatives - nextStartIndex)
            listOf(
                renderHeader(alternativeNumberedPlans, totalNumAlternatives),
                renderAlternatives(alternativeNumberedPlans, configuration),
                renderShowMoreButton(nextSize)
            ).flatten()
        }
    }

    private fun renderHeader(alternativeNumberedPlans: List<Pair<Int, Planner.Plan>>, totalNumAlternatives: Int) =
        listOf(
            TextDisplay.of(
                "Alternatives " +
                        alternativeNumberedPlans.first().component1().toString() +
                        " through " +
                        alternativeNumberedPlans.last().component1().toString() +
                        " out of $totalNumAlternatives:"
            )
        )

    private fun renderAlternatives(alternativeNumberedPlans: List<Pair<Int, Planner.Plan>>, configuration: Configuration) =
        alternativeNumberedPlans.map { (alternativeNumber, plan) ->
            renderAlternative(alternativeNumber, plan, configuration)
        }

    private fun renderAlternative(alternativeNumber: Int, plan: Planner.Plan, configuration: Configuration) = Container.of(
        Section.of(
            Buttons.SuggestAlternative(
                alternativeNumber = alternativeNumber,
                alternativeHashcode = plan.hashCode(),
                screen = this@PlanAlternativeListScreen
            ).render(),
            TextDisplay.of(AlternativePrinter(configuration).printAlternative(alternativeNumber, plan))
        )
    )

    private fun renderShowMoreButton(nextSize: Int): List<MessageTopLevelComponent> = if (nextSize > 0) {
        listOf(
            ActionRow.of(
                Buttons.ShowMoreAlternatives(
                    nextSize = nextSize,
                    screen = this@PlanAlternativeListScreen
                ).render()
            )
        )
    } else emptyList()

    override fun parameters(): List<String> =
        listOf(weekMondayDate.toString(), startIndex.toString(), size.toString())

    companion object {
        fun reconstruct(parameters: List<String>, guildId: Long) = PlanAlternativeListScreen(
            weekMondayDate = LocalDate.parse(parameters[0]),
            startIndex = parameters[1].toInt(),
            size = parameters[2].toInt(),
            guildId = guildId
        )
    }

    class Buttons {
        class SuggestAlternative(
            val alternativeNumber: Int,
            val alternativeHashcode: Int,
            screen: PlanAlternativeListScreen
        ) : ScreenButton<PlanAlternativeListScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Suggest this")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddPublicScreen {
                    PlanAlternativeSuggestedScreen(
                        weekMondayDate = screen.weekMondayDate,
                        alternativeNumber = alternativeNumber,
                        suggestingUserId = event.user.idLong,
                        guildId = screen.guildId
                    )
                }
            }

            override fun parameters(): List<String> =
                listOf(alternativeNumber.toString(), alternativeHashcode.toString())

            object Reconstructor :
                ScreenComponentReconstructor<PlanAlternativeListScreen, SuggestAlternative>(
                    screenClass = PlanAlternativeListScreen::class,
                    componentClass = SuggestAlternative::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    SuggestAlternative(
                        alternativeNumber = componentParameters[0].toInt(),
                        alternativeHashcode = componentParameters[1].toInt(),
                        screen = reconstruct(
                            parameters = screenParameters,
                            guildId = guildId
                        )
                    )
            }
        }

        class ShowMoreAlternatives(val nextSize: Int, screen: PlanAlternativeListScreen) :
            ScreenButton<PlanAlternativeListScreen>(
                screen = screen
            ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Show $nextSize more...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndAddEphemeralScreen {
                    PlanAlternativeListScreen(
                        weekMondayDate = screen.weekMondayDate,
                        startIndex = screen.startIndex + screen.size,
                        size = nextSize,
                        guildId = screen.guildId
                    )
                }
            }

            override fun parameters(): List<String> = listOf(nextSize.toString())

            object Reconstructor :
                ScreenComponentReconstructor<PlanAlternativeListScreen, ShowMoreAlternatives>(
                    screenClass = PlanAlternativeListScreen::class,
                    componentClass = ShowMoreAlternatives::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    ShowMoreAlternatives(
                        nextSize = componentParameters.first().toInt(),
                        screen = reconstruct(
                            parameters = screenParameters,
                            guildId = guildId
                        )
                    )
            }
        }
    }
}
