package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.MDC_GUILD_ID
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityReminderScreen
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.MDC
import java.time.ZoneOffset
import java.util.*

class ReminderJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Reminder for next week's message triggered")
        ConfigurationRepository.findAllGuildIds().forEach { guildId ->
            MDC.putCloseable(MDC_GUILD_ID, guildId.toString()).use {
                postReminderMessage(guildId)
            }
        }
    }

    private fun postReminderMessage(guildId: Long) {
        val configuration = ConfigurationRepository.loadOrInitialize(guildId)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else {
            val channelId = configuration.channelId
            if (channelId != null) {
                val channel = JDAHolder.jda.getTextChannelById(channelId)
                val weekMondayDate = nextMonday()
                logger.info("Sending any reminders for the week of Monday $weekMondayDate")
                val discordMessageId =
                    WeekRepository.loadOrInitialize(
                        guildId = configuration.guildId,
                        mondayDate = weekMondayDate
                    ).weekAvailabilityMessageDiscordId
                if (discordMessageId == null) {
                    logger.warn("No message to nag about!")
                } else {
                    channel!!.sendMessage(
                        AvailabilityReminderScreen(
                            weekMondayDate = weekMondayDate,
                            guildId = configuration.guildId
                        ).render()
                    ).queue()
                }
            }
        }
    }

    companion object {
        val cronSchedule =
            CronScheduleBuilder
                .cronSchedule("0 0 16 ? * THU-SUN")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
