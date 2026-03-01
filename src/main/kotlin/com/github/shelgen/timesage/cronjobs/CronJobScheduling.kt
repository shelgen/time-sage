package com.github.shelgen.timesage.cronjobs

import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

object CronJobScheduling {
    fun setUp() {
        val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler().also(Scheduler::start)

        scheduler.scheduleJob(
            JobBuilder.newJob(SendNextWeeksAvailabilityMessageJob::class.java).build(),
            TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(SendNextWeeksAvailabilityMessageJob.cronSchedule)
                .build()
        )

        scheduler.scheduleJob(
            JobBuilder.newJob(SendNextMonthAvailabilityMessageJob::class.java).build(),
            TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(SendNextMonthAvailabilityMessageJob.cronSchedule)
                .build()
        )

        scheduler.scheduleJob(
            JobBuilder.newJob(ReminderJob::class.java).build(),
            TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(ReminderJob.cronSchedule)
                .build()
        )
    }
}
