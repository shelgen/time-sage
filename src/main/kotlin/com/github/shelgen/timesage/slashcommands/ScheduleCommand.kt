package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.plannedWeekStartDate
import com.github.shelgen.timesage.plannedYearMonth
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.PlanAlternativeListScreen
import com.github.shelgen.timesage.ui.screens.PlanAlternativeMonthListScreen
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

object ScheduleCommand : AbstractSlashCommand(
    name = "tsschedule",
    description = "Attempt to schedule next week's or month's sessions",
) {
    override fun handle(event: SlashCommandInteractionEvent, context: OperationContext) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            val configuration = ConfigurationRepository.loadOrInitialize(context)
            val screen = when (configuration.scheduling.type) {
                SchedulingType.MONTHLY -> PlanAlternativeMonthListScreen(
                    yearMonth = plannedYearMonth(configuration.timeZone, configuration.scheduling.daysBeforePeriod),
                    startIndex = 0,
                    size = 3,
                    context = context
                )
                SchedulingType.WEEKLY -> PlanAlternativeListScreen(
                    weekStartDate = plannedWeekStartDate(
                        configuration.scheduling.startDayOfWeek,
                        configuration.timeZone,
                        configuration.scheduling.daysBeforePeriod,
                    ),
                    startIndex = 0,
                    size = 3,
                    context = context
                )
            }
            it.sendMessage(screen.render()).queue()
            MDC.clear()
        }
    }
}

