package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.MDC_GUILD_ID
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.MDC.putCloseable
import java.time.ZoneOffset
import java.util.*

class SendNextWeeksAvailabilityMessageJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Schedule for sending next week's message triggered")
        ConfigurationRepository.findAllGuildIds().forEach { guildId ->
            putCloseable(MDC_GUILD_ID, guildId.toString()).use {
                AvailabilityMessageSender.postAvailabilityMessage(guildId)
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
