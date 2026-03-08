package com.github.shelgen.timesage.cronjobs

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.planning.AvailabilityInterface
import com.github.shelgen.timesage.planning.AvailabilityMessageSender
import com.github.shelgen.timesage.planning.PlanningProcess
import com.github.shelgen.timesage.planning.SentReminder
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.PlanningProcessRepository
import com.github.shelgen.timesage.withTenantMDC
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class HourlyJob : Job {
    override fun execute(jobExecutionContext: JobExecutionContext) {
        logger.info("Hourly job triggered")
        ConfigurationRepository.all().forEach { configuration ->
            withTenantMDC(configuration.tenant) {
                executeForTenant(configuration)
            }
        }
    }

    private fun executeForTenant(configuration: Configuration) {
        logger.info("Doing hourly work for tenant")

        val currentDate = configuration.localization.currentDate()
        val currentHour = configuration.localization.currentHour()

        with(configuration.periodicPlanning) {
            if (enabled && currentHour == hourOfDay) {
                logger.info("Periodic planning - checking if the next period's planning should be started")
                val dateRange = nextPeriod(configuration.localization)
                if (currentDate == dateRange.fromInclusive.minusDays(daysInAdvance.toLong())) {
                    AvailabilityMessageSender.postAvailabilityInterface(dateRange, configuration.tenant)
                }
            }
        }

        with(configuration.reminders) {
            if (enabled && currentHour == hourOfDay) {
                PlanningProcessRepository.loadAll(configuration.tenant)
                    .filter { it.state == PlanningProcess.State.COLLECTING_AVAILABILITIES }
                    .forEach { planningProcess ->
                        val availabilityInterface = planningProcess.availabilityInterface ?: return@forEach
                        val lastMessageTimestamp =
                            planningProcess.sentReminders.maxOfOrNull(SentReminder::sentAt)
                                ?: availabilityInterface.postedAt
                        val lastMessageDate = configuration.localization.dateOf(lastMessageTimestamp)
                        if (currentDate == lastMessageDate.plusDays(intervalDays.toLong())) {
                            sendReminder(planningProcess, availabilityInterface, configuration)
                        }
                    }
            }
        }
    }

    private fun sendReminder(planningProcess: PlanningProcess, availabilityInterface: AvailabilityInterface, configuration: Configuration) {
        val uansweredUsers = planningProcess.usersThatHaventAnswered(configuration)
        if (uansweredUsers.isNotEmpty()) {
            logger.info("Sending reminder for ${planningProcess.dateRange}")
            JDAHolder.getTextChannel(configuration.tenant).sendMessage(
                MessageCreateBuilder().setContent(
                    "Hey " +
                            uansweredUsers.joinToString(
                                separator = ", ",
                                transform = DiscordUserId::toMention
                            ) +
                            "!" +
                            " Looks like I'm missing your availability for the schedule." +
                            " Could you check it out? " +
                            availabilityInterface.toLink(configuration.tenant)
                ).build()
            ).queue { message ->
                PlanningProcessRepository.update(planningProcess) {
                    it.sentReminders.add(SentReminder(Instant.now(), DiscordMessageId(message.idLong)))
                }
            }
        }
    }

    companion object {
        val CRON_SCHEDULE: CronScheduleBuilder =
            CronScheduleBuilder
                .cronSchedule("0 0 * * * ?")
                .inTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }
}
