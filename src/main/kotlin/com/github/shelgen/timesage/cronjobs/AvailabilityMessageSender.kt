package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.screens.PlayerAvailabilityScreen

object AvailabilityMessageSender {
    fun postAvailabilityMessage(guildId: Long) {
        val configuration = ConfigurationRepository.loadOrInitialize(guildId)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else {
            val channelId = configuration.channelId
            if (channelId == null) {
                logger.info("No channel configured, not sending an availability message")
            } else {
                val channel = JDAHolder.jda.getTextChannelById(channelId)
                val weekMondayDate = nextMonday()
                val discordMessageId =
                    WeekRepository.loadOrInitialize(
                        guildId = configuration.guildId,
                        mondayDate = weekMondayDate
                    ).weekAvailabilityMessageDiscordId
                if (discordMessageId == null) {
                    logger.info("Sending availability messsage for the week of Monday $weekMondayDate")
                    channel!!.sendMessage(
                        PlayerAvailabilityScreen(
                            weekMondayDate = nextMonday(),
                            guildId = configuration.guildId
                        ).render()
                    ).queue { message ->
                        val messageId = message.idLong
                        WeekRepository.update(guildId = configuration.guildId, mondayDate = weekMondayDate) {
                            it.weekAvailabilityMessageDiscordId = messageId
                        }
                    }
                } else {
                    logger.info("Availability messsage for the week of Monday $weekMondayDate has already been sent")
                }
            }
        }
    }
}
