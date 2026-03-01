package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.plannedYearMonth
import com.github.shelgen.timesage.repositories.AvailabilitiesMonthRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityMonthPageScreen
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.format.DateTimeFormatter
import java.util.*

object MonthlyAvailabilityMessageSender {
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    fun postAvailabilityMessage(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a monthly message.")
            return
        }
        if (configuration.scheduling.type != SchedulingType.MONTHLY) {
            logger.info("Not configured for monthly scheduling, skipping.")
            return
        }

        val yearMonth = plannedYearMonth(configuration.timeZone, configuration.scheduling.daysBeforePeriod)
        val existing = AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = context)
        if (existing.headerMessageId != null) {
            logger.info("Monthly availability message for $yearMonth has already been sent.")
            return
        }

        val channel = JDAHolder.jda.getTextChannelById(context.channelId) ?: run {
            logger.warn("Could not find text channel ${context.channelId}")
            return
        }

        logger.info("Sending monthly availability message for $yearMonth")

        val datePeriod = DatePeriod.monthFrom(yearMonth)
        val allTimeSlots = configuration.scheduling.getTimeSlots(datePeriod, configuration.timeZone)
        val slotsPerPage = AvailabilityMonthPageScreen.slotsPerPage()
        val pageCount = maxOf(1, (allTimeSlots.size + slotsPerPage - 1) / slotsPerPage)

        val headerText = "## Monthly availability for ${yearMonth.format(monthFormatter)}\n" +
                "Please fill in your availability in the thread below. " +
                "There ${if (pageCount == 1) "is **1 page**" else "are **$pageCount pages**"} of time slots."

        channel.sendMessage(
            MessageCreateBuilder()
                .setContent(headerText)
                .build()
        ).queue { headerMessage ->
            AvailabilitiesMonthRepository.update(yearMonth = yearMonth, context = context) {
                it.headerMessageId = headerMessage.idLong
            }

            headerMessage.createThreadChannel("${yearMonth.format(monthFormatter)} availability")
                .queue { thread ->
                    AvailabilitiesMonthRepository.update(yearMonth = yearMonth, context = context) {
                        it.threadId = thread.idLong
                    }

                    for (pageIndex in 0 until pageCount) {
                        thread.sendMessage(
                            AvailabilityMonthPageScreen(
                                yearMonth = yearMonth,
                                pageIndex = pageIndex,
                                mainChannelId = context.channelId,
                                context = context
                            ).render()
                        ).queue()
                    }
                }
        }
    }
}
