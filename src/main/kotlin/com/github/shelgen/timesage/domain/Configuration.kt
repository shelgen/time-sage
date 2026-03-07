package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

open class Configuration(
    open val enabled: Boolean,
    open val localization: Localization,
    open val scheduling: Scheduling,
    open val activities: List<Activity>,
    open val voiceChannelId: Long?
) {
    open fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }

    fun activePeriod(): TargetPeriod {
        val today = LocalDate.now(localization.timeZone.toZoneId())
        val lookAhead = today.plusDays(scheduling.numDaysInAdvanceToStartPlanning.toLong())
        return when (scheduling.type) {
            SchedulingType.WEEKLY -> {
                val daysBack =
                    (DayOfWeek.entries.size + lookAhead.dayOfWeek.value - localization.startDayOfWeek.value) % DayOfWeek.entries.size
                DateRange.weekFrom(lookAhead.minusDays(daysBack.toLong()))
            }

            SchedulingType.MONTHLY -> DateRange.from(YearMonth.from(lookAhead))
        }
    }

    companion object {
        fun createDefault(tenant: Tenant) = Configuration(
            enabled = false,
            localization = Localization.DEFAULT,
            scheduling = Scheduling.DEFAULT,
            activities = emptyList(),
            voiceChannelId = null
        )
    }
}

class MutableConfiguration(
    override var enabled: Boolean,
    override val localization: MutableLocalization,
    override val scheduling: MutableScheduling,
    override val activities: MutableList<MutableActivity>,
    override var voiceChannelId: Long?
) : Configuration(
    enabled = enabled,
    localization = localization,
    scheduling = scheduling,
    activities = activities,
    voiceChannelId = voiceChannelId
) {
    constructor(configuration: Configuration) : this(
        enabled = configuration.enabled,
        localization = MutableLocalization(configuration.localization),
        scheduling = MutableScheduling(configuration.scheduling),
        activities = configuration.activities.map(::MutableActivity).toMutableList(),
        voiceChannelId = configuration.voiceChannelId
    )

    override fun getActivity(activityId: Int): MutableActivity =
        activities.first { it.id == activityId }

    fun addNewActivity(): MutableActivity {
        val id = (activities.maxOfOrNull { it.id } ?: 0) + 1
        val activity = MutableActivity.createNew(id)
        activities.add(activity)
        return activity
    }
}
