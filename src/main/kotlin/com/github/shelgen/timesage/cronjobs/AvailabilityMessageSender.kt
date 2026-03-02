package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.formatAsShortDate
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityScreen
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

object AvailabilityMessageSender {
    fun postAvailabilityMessage(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
            return
        }

        val period = configuration.scheduling.activePeriod(configuration.timeZone)
        val existing = AvailabilitiesPeriodRepository.loadOrInitialize(period, context)
        if (existing.messageId != null) {
            logger.info("Availability message for $period has already been sent.")
            return
        }

        val channel = JDAHolder.jda.getTextChannelById(context.channelId) ?: run {
            logger.warn("Could not find text channel ${context.channelId}")
            return
        }

        logger.info("Sending availability message for $period")

        val timeSlots = configuration.scheduling.getTimeSlots(period, configuration.timeZone)

        if (timeSlots.size > 7) {
            val periodLabel = periodLabel(period)
            val chunks = AvailabilityScreen.weekChunks(period, configuration.scheduling.startDayOfWeek)
            val messageCount = 1 + chunks.size

            val headerText = "## Availability for $periodLabel\n" +
                    "Please fill in your availability in the thread below."

            channel.sendMessage(
                MessageCreateBuilder().setContent(headerText).build()
            ).queue { headerMessage ->
                AvailabilitiesPeriodRepository.update(period, context) {
                    it.messageId = headerMessage.idLong
                }

                headerMessage.createThreadChannel("$periodLabel availability")
                    .queue { thread ->
                        AvailabilitiesPeriodRepository.update(period, context) {
                            it.threadId = thread.idLong
                        }

                        for (pageIndex in 0 until messageCount) {
                            thread.sendMessage(
                                AvailabilityScreen(
                                    periodStart = period.fromDate,
                                    periodEnd = period.toDate,
                                    pageIndex = pageIndex,
                                    mainChannelId = context.channelId,
                                    context = context
                                ).render()
                            ).queue()
                        }
                        thread.manager.setLocked(true).queue()
                    }
            }
        } else {
            channel.sendMessage(
                AvailabilityScreen(
                    periodStart = period.fromDate,
                    periodEnd = period.toDate,
                    pageIndex = AvailabilityScreen.SINGLE_PAGE,
                    mainChannelId = context.channelId,
                    context = context
                ).render()
            ).queue { message ->
                AvailabilitiesPeriodRepository.update(period, context) {
                    it.messageId = message.idLong
                }
                replaceBotPinsWith(message)
            }
        }
    }

    fun periodLabel(period: DatePeriod): String {
        val ym = YearMonth.from(period.fromDate)
        return if (ym.atDay(1) == period.fromDate && ym.atEndOfMonth() == period.toDate) {
            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        } else {
            "${period.fromDate.formatAsShortDate()} through ${period.toDate.formatAsShortDate()}"
        }
    }
}
