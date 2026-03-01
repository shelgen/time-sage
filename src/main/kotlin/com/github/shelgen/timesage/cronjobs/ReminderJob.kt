package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.nextMonthYearMonth
import com.github.shelgen.timesage.nextWeekStartDate
import com.github.shelgen.timesage.repositories.AvailabilitiesMonthRepository
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import com.github.shelgen.timesage.ui.screens.AvailabilityReminderScreen
import com.github.shelgen.timesage.withContextMDC
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.ZoneOffset
import java.util.*

class ReminderJob : Job {
    override fun execute(context: JobExecutionContext) {
        logger.info("Reminder triggered")
        ConfigurationRepository.findAllOperationContexts().forEach { context ->
            withContextMDC(context) { context ->
                postReminderMessage(context)
            }
        }
    }

    private fun postReminderMessage(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        if (!configuration.enabled) {
            logger.info("Disabled, not sending a reminder.")
            return
        }
        when (configuration.scheduling.type) {
            SchedulingType.WEEKLY -> postWeeklyReminder(context)
            SchedulingType.MONTHLY -> postMonthlyReminder(context)
        }
    }

    private fun postWeeklyReminder(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        val channel = JDAHolder.jda.getTextChannelById(context.channelId)
        val startDate = nextWeekStartDate(configuration.scheduling.startDayOfWeek)
        logger.info("Sending any reminders for the week starting $startDate")
        val week = AvailabilitiesWeekRepository.loadOrInitialize(startDate = startDate, context = context)
        if (week.concluded) {
            logger.info("Planning for the week starting $startDate has been concluded, not sending a reminder.")
        } else if (week.messageId == null) {
            logger.warn("No weekly message to nag about!")
        } else {
            channel!!.sendMessage(
                AvailabilityReminderScreen(
                    startDate = startDate,
                    context = context
                ).render()
            ).queue()
        }
    }

    private fun postMonthlyReminder(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        val channel = JDAHolder.jda.getTextChannelById(context.channelId)
        val yearMonth = nextMonthYearMonth()
        logger.info("Sending any monthly reminders for $yearMonth")
        val month = AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = context)
        if (month.concluded) {
            logger.info("Planning for $yearMonth has been concluded, not sending a reminder.")
        } else if (month.headerMessageId == null || month.threadId == null) {
            logger.warn("No monthly availability message to nag about!")
        } else {
            val unansweredParticipants = configuration.activities
                .flatMap(Activity::participants)
                .map(Participant::userId)
                .filter { month.responses.forUserId(it) == null }
                .distinct()
                .sorted()
            if (unansweredParticipants.isNotEmpty()) {
                val threadUrl = "https://discord.com/channels/${context.guildId}/${month.threadId}"
                channel!!.sendMessage(
                    MessageCreateBuilder().setContent(
                        "Hey ${unansweredParticipants.joinToString(separator = ", ") { DiscordFormatter.mentionUser(it) }}!" +
                                " Looks like I'm missing your availability for next month's schedule." +
                                " Could you check it out? $threadUrl"
                    ).build()
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

