package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordThreadChannelId
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.time.TimeSlot
import com.github.shelgen.timesage.ui.screens.AvailabilityMessageScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadPeriodLevelScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadStartScreen
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadChunkScreen
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import java.time.Instant

private const val MAX_TIME_SLOTS_IN_SINGLE_MESSAGE = 7

object AvailabilityMessageSender {
    fun postAvailabilityInterface(
        dateRange: DateRange,
        tenant: Tenant
    ) {
        val configuration = ConfigurationRepository.loadOrInitialize(tenant)
        if (PlanningProcessRepository.load(dateRange, tenant) != null) {
            logger.info("Planning process for $dateRange already exists, not sending messages.")
            return
        }

        logger.info("Sending availability message for $dateRange...")
        val timeSlots = configuration.produceTimeSlots(dateRange)

        val planningProcess = PlanningProcess.new(dateRange, tenant, timeSlots)
        PlanningProcessRepository.saveNew(planningProcess)

        val channel = JDAHolder.getTextChannel(tenant)
        if (timeSlots.size > MAX_TIME_SLOTS_IN_SINGLE_MESSAGE) {
            logger.info("More than $MAX_TIME_SLOTS_IN_SINGLE_MESSAGE time slots, creating thread...")
            val chunks = timeSlots.chunked(MAX_TIME_SLOTS_IN_SINGLE_MESSAGE)

            logger.info("Sending thread start message...")
            channel.sendMessage(AvailabilityThreadStartScreen(dateRange, tenant).render()).queue { headerMessage ->
                logger.info("Creating thread channel...")
                headerMessage.createThreadChannel("${dateRange.toLocalizedString(configuration.localization)} availability")
                    .queue { threadChannel ->
                        logger.info("Sending period level message...")
                        threadChannel.sendMessage(AvailabilityThreadPeriodLevelScreen(dateRange, tenant).render())
                            .queue { introMessage ->
                                sendChunksAndPersist(
                                    planningProcess = planningProcess,
                                    thread = threadChannel,
                                    dateRange = dateRange,
                                    tenant = tenant,
                                    timeSlots = timeSlots,
                                    chunks = chunks,
                                    chunkIndex = 0,
                                    threadStartMessageId = DiscordMessageId(headerMessage.idLong),
                                    periodLevelMessageId = DiscordMessageId(introMessage.idLong),
                                    accumulatedChunkMessageIds = emptyList(),
                                )
                            }
                    }
            }
        } else {
            logger.info("Sending availability message...")
            channel.sendMessage(AvailabilityMessageScreen(dateRange, tenant).render()).queue { message ->
                logger.info("Saving availability interface...")
                val availabilityInterface = AvailabilityMessage(Instant.now(), DiscordMessageId(message.idLong))
                PlanningProcessRepository.update(planningProcess) { it.startCollectingAvailabilities(availabilityInterface) }
                logger.info("Updating pinned messages...")
                JDAHolder.pin(availabilityInterface, tenant)
            }
        }
    }

    private fun sendChunksAndPersist(
        planningProcess: PlanningProcess,
        thread: ThreadChannel,
        dateRange: DateRange,
        tenant: Tenant,
        timeSlots: List<TimeSlot>,
        chunks: List<List<TimeSlot>>,
        chunkIndex: Int,
        threadStartMessageId: DiscordMessageId,
        periodLevelMessageId: DiscordMessageId,
        accumulatedChunkMessageIds: List<DiscordMessageId>,
    ) {
        if (chunkIndex >= chunks.size) {
            logger.info("Saving availability interface...")
            val availabilityInterface = AvailabilityThread(
                postedAt = Instant.now(),
                threadStartMessage = threadStartMessageId,
                threadChannel = DiscordThreadChannelId(thread.idLong),
                periodLevelMessage = periodLevelMessageId,
                chunkMessages = accumulatedChunkMessageIds,
            )
            PlanningProcessRepository.update(planningProcess) { it.startCollectingAvailabilities(availabilityInterface) }
            logger.info("Locking thread...")
            thread.manager.setLocked(true).queue()
            JDAHolder.pin(availabilityInterface, tenant)
            return
        }

        logger.info("Sending chunk message $chunkIndex...")
        thread.sendMessage(
            AvailabilityThreadChunkScreen(
                chunkIndex = chunkIndex,
                dateRange = dateRange,
                tenant = tenant
            ).render()
        ).queue { chunkMessage ->
            sendChunksAndPersist(
                planningProcess = planningProcess,
                thread = thread,
                dateRange = dateRange,
                tenant = tenant,
                timeSlots = timeSlots,
                chunks = chunks,
                chunkIndex = chunkIndex + 1,
                threadStartMessageId = threadStartMessageId,
                periodLevelMessageId = periodLevelMessageId,
                accumulatedChunkMessageIds = accumulatedChunkMessageIds + DiscordMessageId(chunkMessage.idLong),
            )
        }
    }
}
