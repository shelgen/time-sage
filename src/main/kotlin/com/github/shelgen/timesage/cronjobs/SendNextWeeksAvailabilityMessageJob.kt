package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.withContextMDC
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class SendNextWeeksAvailabilityMessageJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Schedule for sending next week's message triggered")
        ConfigurationRepository.findAllOperationContexts().forEach { context ->
            withContextMDC(context) { context ->
                AvailabilityMessageSender.postAvailabilityMessage(context)
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
