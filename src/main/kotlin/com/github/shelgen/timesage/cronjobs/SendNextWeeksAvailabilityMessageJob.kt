package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextMonday
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.screens.PlayerAvailabilityScreen
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class SendNextWeeksAvailabilityMessageJob : Job {
    override fun execute(context: JobExecutionContext) {
        sendMessage()
    }

    fun sendMessage() {
        logger.info("Schedule for sending next week's message triggered")
        val configuration = ConfigurationRepository.load()
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else {
            val channelId = configuration.channelId
            if (channelId != null) {
                val channel = JDAHolder.jda.getTextChannelById(channelId)
                channel!!.sendMessage(PlayerAvailabilityScreen(nextMonday()).render()).queue()
            }
        }
    }

    companion object {
        val cronSchedule =
            CronScheduleBuilder
                .cronSchedule("0 0 16 ? * WED")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
