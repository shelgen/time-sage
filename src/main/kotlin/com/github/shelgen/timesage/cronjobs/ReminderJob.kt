package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextWeekStartDate
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import com.github.shelgen.timesage.ui.screens.AvailabilityReminderScreen
import com.github.shelgen.timesage.withContextMDC
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class ReminderJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Reminder for next week's message triggered")
        ConfigurationRepository.findAllOperationContexts().forEach { context ->
            withContextMDC(context) { context ->
                postReminderMessage(context)
            }
        }
    }

    private fun postReminderMessage(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a message.")
        } else {
            val channel = JDAHolder.jda.getTextChannelById(context.channelId)
            val startDate = nextWeekStartDate(configuration.scheduling.startDayOfWeek)
            logger.info("Sending any reminders for the week starting $startDate")
            val discordMessageId =
                AvailabilitiesWeekRepository.loadOrInitialize(
                    startDate = startDate,
                    context = context
                ).messageId
            if (discordMessageId == null) {
                logger.warn("No message to nag about!")
            } else {
                channel!!.sendMessage(
                    AvailabilityReminderScreen(
                        startDate = startDate,
                        context = context
                    ).render()
                ).queue()
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
