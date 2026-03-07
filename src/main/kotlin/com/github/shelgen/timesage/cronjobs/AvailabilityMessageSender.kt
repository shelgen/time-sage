package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.AvailabilityMessageOrThread
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityMessageScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadWeekScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadPeriodLevelScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadStartScreen
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import java.time.LocalDate

object AvailabilityMessageSender {
    fun postAvailabilityMessage(tenant: Tenant) {
        val configuration = ConfigurationRepository.loadOrInitialize(tenant)
        if (!configuration.enabled) {
            logger.info("Time sage is disabled. Not sending a message.")
            return
        }

        val dateRange = configuration.activePeriod()
        val existing = AvailabilitiesPeriodRepository.loadOrInitialize(dateRange, tenant)
        if (existing.availabilityMessageOrThread != null) {
            logger.info("Availability message for $dateRange has already been sent.")
            return
        }

        val channel = JDAHolder.jda.getTextChannelById(tenant.channelId) ?: run {
            logger.warn("Could not find text channel ${tenant.channelId}")
            return
        }

        logger.info("Sending availability message for $dateRange")

        val timeSlots =
            configuration.scheduling.timeSlotRules.getTimeSlots(dateRange, configuration.localization.timeZone)

        if (timeSlots.size > 7) {
            val chunks = dateRange.chunkedByWeek(configuration.localization.startDayOfWeek)

            channel.sendMessage(AvailabilityThreadStartScreen(dateRange, tenant).render()).queue { headerMessage ->
                headerMessage.createThreadChannel("${dateRange.toLocalizedString(configuration.localization)} availability")
                    .queue { thread ->
                        thread.sendMessage(AvailabilityThreadPeriodLevelScreen(dateRange, tenant).render())
                            .queue { introMessage ->
                                sendChunksAndPersist(
                                    thread = thread,
                                    dateRange = dateRange,
                                    tenant = tenant,
                                    weekChunks = chunks,
                                    weekChunkIndex = 0,
                                    headerMessageId = headerMessage.idLong,
                                    introMessageId = introMessage.idLong,
                                    accumulatedIds = emptyMap(),
                                )
                            }
                    }
            }
        } else {
            channel.sendMessage(AvailabilityMessageScreen(dateRange, tenant).render()).queue { message ->
                AvailabilitiesPeriodRepository.update(dateRange, tenant) {
                    it.availabilityMessageOrThread = AvailabilityMessageOrThread.AvailabilityMessage(message.idLong)
                }
                replaceBotPinsWith(message)
            }
        }
    }

    private fun sendChunksAndPersist(
        thread: ThreadChannel,
        dateRange: DateRange,
        tenant: Tenant,
        weekChunks: List<List<LocalDate>>,
        weekChunkIndex: Int,
        headerMessageId: Long,
        introMessageId: Long,
        accumulatedIds: Map<DateRange, Long>,
    ) {
        if (weekChunkIndex >= weekChunks.size) {
            AvailabilitiesPeriodRepository.update(dateRange, tenant) {
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

        val weekChunk = weekChunks[weekChunkIndex]
        val chunkPeriod = DateRange(weekChunk.first(), weekChunk.last())
        thread.sendMessage(
            AvailabilityThreadWeekScreen(
                weekChunkIndex = weekChunkIndex,
                dateRange = dateRange,
                tenant = tenant
            ).render()
        ).queue { chunkMessage ->
            sendChunksAndPersist(
                thread = thread,
                dateRange = dateRange,
                tenant = tenant,
                weekChunks = weekChunks,
                weekChunkIndex = weekChunkIndex + 1,
                headerMessageId = headerMessageId,
                introMessageId = introMessageId,
                accumulatedIds = accumulatedIds + (chunkPeriod to chunkMessage.idLong),
            )
        }
    }
}
