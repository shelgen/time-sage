package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.AvailabilityMessageOrThread
import com.github.shelgen.timesage.domain.Tenant
import com.github.shelgen.timesage.domain.ActivityMember
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.withTenantMDC
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class HourlyPlanningJob : Job {
    override fun execute(jobExecutionContext: JobExecutionContext) {
        logger.info("Hourly planning job triggered")
        ConfigurationRepository.findAllTenants().forEach { tenant ->
            withTenantMDC(tenant) { tenant ->
                checkAndActForChannel(tenant)
            }
        }
    }

    private fun checkAndActForChannel(tenant: Tenant) {
        val configuration = ConfigurationRepository.loadOrInitialize(tenant)
        if (!configuration.enabled) return

        val currentHour = ZonedDateTime.now(configuration.localization.timeZone.toZoneId()).hour
        if (currentHour != configuration.scheduling.timeOfDayToStartPlanning) return

        val period = configuration.activePeriod()
        val today = LocalDate.now(configuration.localization.timeZone.toZoneId())
        logger.info("Checking planning for period $period")

        val data = AvailabilitiesPeriodRepository.loadOrInitialize(period, tenant)

        if (data.availabilityMessageOrThread == null) {
            AvailabilityMessageSender.postAvailabilityMessage(tenant)
            return
        }

        if (data.concluded) {
            logger.info("Planning for $period has been concluded, not sending a reminder.")
            return
        }

        val intervalDays = configuration.scheduling.reminderIntervalDays
        if (intervalDays == 0) return

        val planningStartDate = period.fromInclusive.minusDays(configuration.scheduling.numDaysInAdvanceToStartPlanning.toLong())
        val daysSincePlanningStart = today.toEpochDay() - planningStartDate.toEpochDay()
        if (daysSincePlanningStart <= 0) return
        if (daysSincePlanningStart % intervalDays != 0L) return
        if (data.lastReminderDate == today) return

        val unansweredParticipants = configuration.activities
            .asSequence()
            .flatMap(Activity::members)
            .map(ActivityMember::userId)
            .filter { data.availabilityResponses[it] == null }
            .distinct()
            .sorted()
            .toList()
        if (unansweredParticipants.isEmpty()) return

        logger.info("Sending reminder for period $period")
        val messageUrl = when (val ref = data.availabilityMessageOrThread) {
            is AvailabilityMessageOrThread.AvailabilityThread ->
                "https://discord.com/channels/${tenant.guildId}/${ref.threadChannelId}"
            is AvailabilityMessageOrThread.AvailabilityMessage ->
                "https://discord.com/channels/${tenant.guildId}/${tenant.channelId}/${ref.screenMessageId}"
            null -> return
        }

        JDAHolder.jda.getTextChannelById(tenant.channelId)!!.sendMessage(
            MessageCreateBuilder().setContent(
                "Hey ${unansweredParticipants.joinToString(separator = ", ") { DiscordFormatter.mentionUser(it) }}!" +
                        " Looks like I'm missing your availability for the schedule." +
                        " Could you check it out? $messageUrl"
            ).build()
        ).queue()
        AvailabilitiesPeriodRepository.update(period, tenant) {
            it.lastReminderDate = today
        }
    }

    companion object {
        val CRON_SCHEDULE: CronScheduleBuilder =
            CronScheduleBuilder
                .cronSchedule("0 0 * * * ?")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
