package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityScreen

object AvailabilityMessageSender {
    fun postAvailabilityMessage(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else {
            val channel = JDAHolder.jda.getTextChannelById(context.channelId)
            val weekMondayDate = nextMonday()
            val existingMessageId =
                WeekRepository.loadOrInitialize(
                    mondayDate = weekMondayDate,
                    context = context
                ).messageDiscordId
            if (existingMessageId == null) {
                logger.info("Sending availability messsage for the week of Monday $weekMondayDate")
                channel!!.sendMessage(
                    AvailabilityScreen(
                        weekMondayDate = nextMonday(),
                        context = context
                    ).render()
                ).queue { message ->
                    val messageId = message.idLong
                    WeekRepository.update(mondayDate = weekMondayDate, context = context) {
                        it.messageDiscordId = messageId
                    }
                }
            } else {
                logger.info("Availability messsage for the week of Monday $weekMondayDate has already been sent")
            }
        }
    }
}
