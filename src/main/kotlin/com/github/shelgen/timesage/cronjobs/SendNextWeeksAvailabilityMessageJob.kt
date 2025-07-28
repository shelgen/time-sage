package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.logger
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class SendNextWeeksAvailabilityMessageJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Schedule for sending next week's message triggered")
        AvailabilityMessageSender.sendMessage()
    }

    companion object {
        val cronSchedule =
            CronScheduleBuilder
                .cronSchedule("0 0 16 ? * WED")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
