package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.AvailabilityMessageOrThread
import com.github.shelgen.timesage.domain.DatePeriod
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.formatAsShortDate
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityScreen
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.LocalDate
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
        if (existing.availabilityMessageOrThread != null) {
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

            val headerText = "## Availability for $periodLabel\n" +
                    "Please fill in your availability in the thread below."

            channel.sendMessage(
                MessageCreateBuilder().setContent(headerText).build()
            ).queue { headerMessage ->
                headerMessage.createThreadChannel("$periodLabel availability")
                    .queue { thread ->
                        thread.sendMessage(
                            AvailabilityScreen(
                                periodStart = period.fromDate,
                                periodEnd = period.toDate,
                                pageIndex = 0,
                                mainChannelId = context.channelId,
                                context = context
                            ).render()
                        ).queue { introMessage ->
                            sendChunksAndPersist(
                                thread = thread,
                                period = period,
                                context = context,
                                chunks = chunks,
                                chunkIndex = 0,
                                headerMessageId = headerMessage.idLong,
                                introMessageId = introMessage.idLong,
                                accumulatedIds = emptyMap(),
                            )
                        }
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
                    it.availabilityMessageOrThread = AvailabilityMessageOrThread.AvailabilityMessage(message.idLong)
                }
                replaceBotPinsWith(message)
            }
        }
    }

    private fun sendChunksAndPersist(
        thread: ThreadChannel,
        period: DatePeriod,
        context: OperationContext,
        chunks: List<List<LocalDate>>,
        chunkIndex: Int,
        headerMessageId: Long,
        introMessageId: Long,
        accumulatedIds: Map<DatePeriod, Long>,
    ) {
        if (chunkIndex >= chunks.size) {
            AvailabilitiesPeriodRepository.update(period, context) {
                it.availabilityMessageOrThread = AvailabilityMessageOrThread.AvailabilityThread(
                    headerMessageId = headerMessageId,
                    threadId = thread.idLong,
                    sessionLimitAndUnavailableMessageId = introMessageId,
                    availabilityMessageIds = accumulatedIds,
                )
            }
            thread.manager.setLocked(true).queue()
            return
        }

        val chunk = chunks[chunkIndex]
        val chunkPeriod = DatePeriod(chunk.first(), chunk.last())
        thread.sendMessage(
            AvailabilityScreen(
                periodStart = period.fromDate,
                periodEnd = period.toDate,
                pageIndex = chunkIndex + 1,
                mainChannelId = context.channelId,
                context = context
            ).render()
        ).queue { chunkMessage ->
            sendChunksAndPersist(
                thread = thread,
                period = period,
                context = context,
                chunks = chunks,
                chunkIndex = chunkIndex + 1,
                headerMessageId = headerMessageId,
                introMessageId = introMessageId,
                accumulatedIds = accumulatedIds + (chunkPeriod to chunkMessage.idLong),
            )
        }
    }

    fun periodLabel(period: DatePeriod): String =
        if (exactlyCoversAMonth(period)) {
            period.fromDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        } else {
            "${period.fromDate.formatAsShortDate()} through ${period.toDate.formatAsShortDate()}"
        }

    private fun exactlyCoversAMonth(period: DatePeriod): Boolean {
        val yearMonth = YearMonth.from(period.fromDate)
        return yearMonth.atDay(1) == period.fromDate && yearMonth.atEndOfMonth() == period.toDate
    }
}
