package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextWeekStartDate
import com.github.shelgen.timesage.replaceBotPinsWith
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityScreen

object AvailabilityMessageSender {
    fun postAvailabilityMessage(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else if (configuration.scheduling.type == SchedulingType.MONTHLY) {
            logger.info("Monthly scheduling configured — weekly sender skipping this channel.")
        } else {
            val channel = JDAHolder.jda.getTextChannelById(context.channelId)
            val startDate = nextWeekStartDate(configuration.scheduling.startDayOfWeek)
            val existingMessageId =
                AvailabilitiesWeekRepository.loadOrInitialize(
                    startDate = startDate,
                    context = context
                ).messageId
            if (existingMessageId == null) {
                logger.info("Sending availability messsage for the week starting $startDate")
                channel!!.sendMessage(
                    AvailabilityScreen(
                        startDate = nextWeekStartDate(configuration.scheduling.startDayOfWeek),
                        context = context
                    ).render()
                ).queue { message ->
                    val messageId = message.idLong
                    AvailabilitiesWeekRepository.update(startDate = startDate, context = context) {
                        it.messageId = messageId
                    }
                    replaceBotPinsWith(message)
                }
            } else {
                logger.info("Availability messsage for the week starting $startDate has already been sent")
            }
        }
    }
}
