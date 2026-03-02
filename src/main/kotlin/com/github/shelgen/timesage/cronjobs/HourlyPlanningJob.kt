package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.AvailabilityMessageOrThread
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesPeriodRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.withContextMDC
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class HourlyPlanningJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Hourly planning job triggered")
        ConfigurationRepository.findAllOperationContexts().forEach { opContext ->
            withContextMDC(opContext) { opContext ->
                checkAndActForChannel(opContext)
            }
        }
    }

    private fun checkAndActForChannel(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) return

        val currentHour = ZonedDateTime.now(configuration.timeZone.toZoneId()).hour
        if (currentHour != configuration.scheduling.planningStartHour) return

        val period = configuration.scheduling.activePeriod(configuration.timeZone)
        val today = LocalDate.now(configuration.timeZone.toZoneId())
        logger.info("Checking planning for period $period")

        val data = AvailabilitiesPeriodRepository.loadOrInitialize(period, context)

        if (data.availabilityMessageOrThread == null) {
            AvailabilityMessageSender.postAvailabilityMessage(context)
            return
        }

        if (data.concluded) {
            logger.info("Planning for $period has been concluded, not sending a reminder.")
            return
        }

        val intervalDays = configuration.scheduling.reminderIntervalDays
        if (intervalDays == 0) return

        val planningStartDate = period.fromDate.minusDays(configuration.scheduling.daysBeforePeriod.toLong())
        val daysSincePlanningStart = today.toEpochDay() - planningStartDate.toEpochDay()
        if (daysSincePlanningStart <= 0) return
        if (daysSincePlanningStart % intervalDays != 0L) return
        if (data.lastReminderDate == today) return

        val unansweredParticipants = configuration.activities
            .flatMap(Activity::participants)
            .map(Participant::userId)
            .filter { data.responses.forUserId(it) == null }
            .distinct()
            .sorted()
        if (unansweredParticipants.isEmpty()) return

        logger.info("Sending reminder for period $period")
        val messageUrl = when (val ref = data.availabilityMessageOrThread) {
            is AvailabilityMessageOrThread.AvailabilityThread ->
                "https://discord.com/channels/${context.guildId}/${ref.threadId}"
            is AvailabilityMessageOrThread.AvailabilityMessage ->
                "https://discord.com/channels/${context.guildId}/${context.channelId}/${ref.messageId}"
            null -> return
        }

        JDAHolder.jda.getTextChannelById(context.channelId)!!.sendMessage(
            MessageCreateBuilder().setContent(
                "Hey ${unansweredParticipants.joinToString(separator = ", ") { DiscordFormatter.mentionUser(it) }}!" +
                        " Looks like I'm missing your availability for the schedule." +
                        " Could you check it out? $messageUrl"
            ).build()
        ).queue()
        AvailabilitiesPeriodRepository.update(period, context) {
            it.lastReminderDate = today
        }
    }

    companion object {
        val cronSchedule =
            CronScheduleBuilder
                .cronSchedule("0 0 * * * ?")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
