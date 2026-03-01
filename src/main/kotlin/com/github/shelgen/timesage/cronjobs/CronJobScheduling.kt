package com.github.shelgen.timesage.cronjobs

import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

object CronJobScheduling {
    fun setUp() {
        val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler().also(Scheduler::start)

        scheduler.scheduleJob(
            JobBuilder.newJob(HourlyPlanningJob::class.java).build(),
            TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(HourlyPlanningJob.cronSchedule)
                .build()
        )
    }
}
