package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.withContextMDC
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class SendNextMonthAvailabilityMessageJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Schedule for sending next month's message triggered")
        ConfigurationRepository.findAllOperationContexts().forEach { context ->
            withContextMDC(context) { context ->
                MonthlyAvailabilityMessageSender.postAvailabilityMessage(context)
            }
        }
    }

    companion object {
        /** Fires at 12:00 UTC on the 22nd of each month to prepare for the following month. */
        val cronSchedule =
            CronScheduleBuilder
                .cronSchedule("0 0 12 22 * ?")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
