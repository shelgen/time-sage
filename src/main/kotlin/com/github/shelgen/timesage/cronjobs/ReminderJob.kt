package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.WeekRepository
import com.github.shelgen.timesage.ui.screens.PlayerAvailabilityReminderScreen
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class ReminderJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Reminder for next week's message triggered")
        val configuration = ConfigurationRepository.load()
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else {
            val channelId = configuration.channelId
            if (channelId != null) {
                val channel = JDAHolder.jda.getTextChannelById(channelId)
                val weekMondayDate = nextMonday()
                logger.info("Sending any reminders for the week of Monday $weekMondayDate")
                val discordMessageId = WeekRepository.load(weekMondayDate).weekAvailabilityMessageDiscordId
                if (discordMessageId == null) {
                    logger.warn("No message to nag about!")
                } else {
                    channel!!.sendMessage(PlayerAvailabilityReminderScreen(weekMondayDate).render()).queue()
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
