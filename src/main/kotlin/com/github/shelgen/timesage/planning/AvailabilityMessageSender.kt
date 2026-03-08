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
import com.github.shelgen.timesage.ui.screens.AvailabilityThreadWeekScreen
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import java.time.Instant

private const val MAX_TIME_SLOTS_IN_SINGLE_MESSAGE = 7

object AvailabilityMessageSender {
    fun postAvailabilityInterface(
        dateRange: DateRange,
        tenant: Tenant
    ) {
        val configuration = ConfigurationRepository.loadOrInitialize(tenant)
        val planningProcess = PlanningProcessRepository.load(dateRange, tenant)
        if (planningProcess != null) {
            logger.info("Planning process for $dateRange already exists, not sending messages.")
            return
        }

        logger.info("Sending availability message for $dateRange...")
        val timeSlots = configuration.produceTimeSlots(dateRange)

        val channel = JDAHolder.getTextChannel(tenant)
        if (timeSlots.size > MAX_TIME_SLOTS_IN_SINGLE_MESSAGE) {
            logger.info("More than $MAX_TIME_SLOTS_IN_SINGLE_MESSAGE time slots, creating thread...")
            val chunks = dateRange.chunkedByWeek(configuration.localization.startDayOfWeek)

            logger.info("Sending thread start message...")
            channel.sendMessage(AvailabilityThreadStartScreen(dateRange, tenant).render()).queue { headerMessage ->
                logger.info("Creating thread channel...")
                headerMessage.createThreadChannel("${dateRange.toLocalizedString(configuration.localization)} availability")
                    .queue { threadChannel ->
                        logger.info("Sending period level message...")
                        threadChannel.sendMessage(AvailabilityThreadPeriodLevelScreen(dateRange, tenant).render())
                            .queue { introMessage ->
                                sendChunksAndPersist(
                                    thread = threadChannel,
                                    dateRange = dateRange,
                                    tenant = tenant,
                                    timeSlots = timeSlots,
                                    weekChunks = chunks,
                                    weekChunkIndex = 0,
                                    threadStartMessageId = DiscordMessageId(headerMessage.idLong),
                                    periodLevelMessageId = DiscordMessageId(introMessage.idLong),
                                    accumulatedWeekMessageIds = emptyMap(),
                                )
                            }
                    }
            }
        } else {
            logger.info("Sending availability message...")
            channel.sendMessage(AvailabilityMessageScreen(dateRange, tenant).render()).queue { message ->
                logger.info("Saving new planning process...")
                val discordMessageId = DiscordMessageId(message.idLong)
                logger.info("Updating pinned messages...")
                val availabilityInterface = AvailabilityMessage(Instant.now(), discordMessageId)
                PlanningProcessRepository.saveNew(
                    PlanningProcess.new(
                        dateRange = dateRange,
                        tenant = tenant,
                        timeSlots = timeSlots,
                        availabilityInterface = availabilityInterface
                    )
                )
                JDAHolder.pin(availabilityInterface, tenant)
            }
        }
    }

    private fun sendChunksAndPersist(
        thread: ThreadChannel,
        dateRange: DateRange,
        tenant: Tenant,
        timeSlots: List<TimeSlot>,
        weekChunks: List<DateRange>,
        weekChunkIndex: Int,
        threadStartMessageId: DiscordMessageId,
        periodLevelMessageId: DiscordMessageId,
        accumulatedWeekMessageIds: Map<DateRange, DiscordMessageId>,
    ) {
        if (weekChunkIndex >= weekChunks.size) {
            logger.info("Saving new planning process...")
            val availabilityInterface = AvailabilityThread(
                postedAt = Instant.now(),
                threadStartMessage = threadStartMessageId,
                threadChannel = DiscordThreadChannelId(thread.idLong),
                periodLevelMessage = periodLevelMessageId,
                weekMessages = accumulatedWeekMessageIds,
            )
            PlanningProcessRepository.saveNew(
                PlanningProcess.new(
                    dateRange = dateRange,
                    tenant = tenant,
                    timeSlots = timeSlots,
                    availabilityInterface = availabilityInterface
                )
            )
            logger.info("Locking thread...")
            thread.manager.setLocked(true).queue()
            JDAHolder.pin(availabilityInterface, tenant)
            return
        }

        val weekChunk = weekChunks[weekChunkIndex]
        logger.info("Sending week message for week chunk $weekChunk...")
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
                timeSlots = timeSlots,
                weekChunks = weekChunks,
                weekChunkIndex = weekChunkIndex + 1,
                threadStartMessageId = threadStartMessageId,
                periodLevelMessageId = periodLevelMessageId,
                accumulatedWeekMessageIds = accumulatedWeekMessageIds + (weekChunk to DiscordMessageId(chunkMessage.idLong)),
            )
        }
    }
}
