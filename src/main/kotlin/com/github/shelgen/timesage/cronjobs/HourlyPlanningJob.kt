package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Activity
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.domain.SchedulingType
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.plannedWeekStartDate
import com.github.shelgen.timesage.plannedYearMonth
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

        when (configuration.scheduling.type) {
            SchedulingType.WEEKLY -> handleWeekly(context)
            SchedulingType.MONTHLY -> handleMonthly(context)
        }
    }

    private fun handleWeekly(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        val startDate = plannedWeekStartDate(
            configuration.scheduling.startDayOfWeek,
            configuration.timeZone,
            configuration.scheduling.daysBeforePeriod,
        )
        val today = LocalDate.now(configuration.timeZone.toZoneId())
        logger.info("Checking weekly planning for the week starting $startDate")

        val week = AvailabilitiesWeekRepository.loadOrInitialize(startDate = startDate, context = context)

        if (week.messageId == null) {
            logger.info("Sending availability message for the week starting $startDate")
            val channel = JDAHolder.jda.getTextChannelById(context.channelId) ?: run {
                logger.warn("Could not find text channel ${context.channelId}")
                return
            }
            channel.sendMessage(
                com.github.shelgen.timesage.ui.screens.AvailabilityScreen(
                    startDate = startDate,
                    context = context
                ).render()
            ).queue { message ->
                AvailabilitiesWeekRepository.update(startDate = startDate, context = context) {
                    it.messageId = message.idLong
                }
                com.github.shelgen.timesage.replaceBotPinsWith(message)
            }
            return
        }

        if (week.concluded) {
            logger.info("Planning for the week starting $startDate has been concluded, not sending a reminder.")
            return
        }

        val intervalDays = configuration.scheduling.reminderIntervalDays
        if (intervalDays == 0) return

        val planningStartDate = startDate.minusDays(configuration.scheduling.daysBeforePeriod.toLong())
        val daysSincePlanningStart = today.toEpochDay() - planningStartDate.toEpochDay()
        if (daysSincePlanningStart <= 0) return
        if (daysSincePlanningStart % intervalDays != 0L) return
        if (week.lastReminderDate == today) return

        logger.info("Sending weekly reminder for the week starting $startDate")
        JDAHolder.jda.getTextChannelById(context.channelId)!!.sendMessage(
            AvailabilityReminderScreen(startDate = startDate, context = context).render()
        ).queue()
        AvailabilitiesWeekRepository.update(startDate = startDate, context = context) {
            it.lastReminderDate = today
        }
    }

    private fun handleMonthly(context: OperationContext) {
        val configuration = ConfigurationRepository.loadOrInitialize(context)
        val yearMonth = plannedYearMonth(configuration.timeZone, configuration.scheduling.daysBeforePeriod)
        val today = LocalDate.now(configuration.timeZone.toZoneId())
        logger.info("Checking monthly planning for $yearMonth")

        val month = AvailabilitiesMonthRepository.loadOrInitialize(yearMonth = yearMonth, context = context)

        if (month.headerMessageId == null) {
            MonthlyAvailabilityMessageSender.postAvailabilityMessage(context)
            return
        }

        if (month.concluded) {
            logger.info("Planning for $yearMonth has been concluded, not sending a reminder.")
            return
        }

        if (month.threadId == null) {
            logger.warn("No monthly thread to remind about!")
            return
        }

        val intervalDays = configuration.scheduling.reminderIntervalDays
        if (intervalDays == 0) return

        val planningStartDate = yearMonth.atDay(1).minusDays(configuration.scheduling.daysBeforePeriod.toLong())
        val daysSincePlanningStart = today.toEpochDay() - planningStartDate.toEpochDay()
        if (daysSincePlanningStart <= 0) return
        if (daysSincePlanningStart % intervalDays != 0L) return
        if (month.lastReminderDate == today) return

        val unansweredParticipants = configuration.activities
            .flatMap(Activity::participants)
            .map(Participant::userId)
            .filter { month.responses.forUserId(it) == null }
            .distinct()
            .sorted()
        if (unansweredParticipants.isEmpty()) return

        logger.info("Sending monthly reminder for $yearMonth")
        val threadUrl = "https://discord.com/channels/${context.guildId}/${month.threadId}"
        JDAHolder.jda.getTextChannelById(context.channelId)!!.sendMessage(
            MessageCreateBuilder().setContent(
                "Hey ${unansweredParticipants.joinToString(separator = ", ") { DiscordFormatter.mentionUser(it) }}!" +
                        " Looks like I'm missing your availability for the schedule." +
                        " Could you check it out? $threadUrl"
            ).build()
        ).queue()
        AvailabilitiesMonthRepository.update(yearMonth = yearMonth, context = context) {
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
