package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.AvailabilityMessage
import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityMessageScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadPeriodLevelScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadStartScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadWeekScreen
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import java.time.LocalDate

object AvailabilityMessageSender {
    fun postAvailabilityMessage(tenant: Tenant) {
        val configuration = ConfigurationRepository.loadOrInitialize(tenant)
        if (!configuration.enabled) {
            logger.info("Time sage is disabled. Not sending a message.")
            return
        }

        val targetPeriod = configuration.activePeriod()
        val existing = AvailabilitiesPeriodRepository.loadOrInitialize(targetPeriod, tenant)
        if (existing.availabilityMessage != null) {
            logger.info("Availability message for $targetPeriod has already been sent.")
            return
        }

        val channel = JDAHolder.jda.getTextChannelById(tenant.channelId) ?: run {
            logger.warn("Could not find text channel ${tenant.channelId}")
            return
        }

        logger.info("Sending availability message for $targetPeriod")

        val timeSlots =
            configuration.scheduling.timeSlotRules.getTimeSlots(targetPeriod, configuration.localization.timeZone)

        if (timeSlots.size > 7) {
            val chunks = targetPeriod.chunkedByWeek(configuration.localization.startDayOfWeek)

            channel.sendMessage(AvailabilityThreadStartScreen(targetPeriod, tenant).render()).queue { headerMessage ->
                headerMessage.createThreadChannel("${targetPeriod.toLocalizedString(configuration.localization)} availability")
                    .queue { threadChannel ->
                        threadChannel.sendMessage(AvailabilityThreadPeriodLevelScreen(targetPeriod, tenant).render())
                            .queue { introMessage ->
                                sendChunksAndPersist(
                                    thread = threadChannel,
                                    targetPeriod = targetPeriod,
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
            channel.sendMessage(AvailabilityMessageScreen(targetPeriod, tenant).render()).queue { message ->
                AvailabilitiesPeriodRepository.update(targetPeriod, tenant) {
                    it.availabilityMessage = AvailabilityMessage.Composite(message.idLong)
                }
                replaceBotPinsWith(message)
            }
        }
    }

    private fun sendChunksAndPersist(
        thread: ThreadChannel,
        targetPeriod: TargetPeriod,
        tenant: Tenant,
        weekChunks: List<List<LocalDate>>,
        weekChunkIndex: Int,
        headerMessageId: Long,
        introMessageId: Long,
        accumulatedIds: Map<DateRange, Long>,
    ) {
        if (weekChunkIndex >= weekChunks.size) {
            AvailabilitiesPeriodRepository.update(targetPeriod, tenant) {
                it.availabilityMessage = AvailabilityMessage.Thread(
                    threadStartScreenMessageId = headerMessageId,
                    threadChannelId = thread.idLong,
                    periodLevelScreenMessageId = introMessageId,
                    availabilityWeekScreenMessageIds = accumulatedIds,
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
                targetPeriod = targetPeriod,
                tenant = tenant
            ).render()
        ).queue { chunkMessage ->
            sendChunksAndPersist(
                thread = thread,
                targetPeriod = targetPeriod,
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
