package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.time.TimeSlot
import java.time.DayOfWeek
import java.time.LocalTime

open class Configuration(
    val tenant: Tenant,
    open val localization: Localization,
    open val activities: List<Activity>,
    open val timeSlotRules: Map<DayOfWeek, LocalTime>,
    open val reminders: Reminders,
    open val periodicPlanning: PeriodicPlanning,
    open val sessionLimit: Int,
) {
    open fun getActivity(activityId: ActivityId): Activity =
        activities.first { it.id == activityId }

    fun produceTimeSlots(dateRange: DateRange): List<TimeSlot> =
        dateRange.dates().mapNotNull { date ->
            timeSlotRules[date.dayOfWeek]
                ?.let { date.atTime(it).atZone(localization.timeZone.toZoneId()).toInstant() }
        }

    companion object {
        fun createDefault(tenant: Tenant) = Configuration(
            tenant = tenant,
            localization = Localization.DEFAULT,
            activities = emptyList(),
            timeSlotRules = emptyMap(),
            reminders = Reminders.DEFAULT,
            periodicPlanning = PeriodicPlanning.DEFAULT,
            sessionLimit = 1,
        )
    }
}

class MutableConfiguration(
    tenant: Tenant,
    override val localization: MutableLocalization,
    override val activities: MutableList<MutableActivity>,
    override val timeSlotRules: MutableMap<DayOfWeek, LocalTime>,
    override val reminders: MutableReminders,
    override val periodicPlanning: MutablePeriodicPlanning,
    override var sessionLimit: Int,
) : Configuration(
    tenant,
    localization = localization,
    activities = activities,
    timeSlotRules = timeSlotRules,
    reminders = reminders,
    periodicPlanning = periodicPlanning,
    sessionLimit = sessionLimit,
) {
    constructor(immutable: Configuration) : this(
        tenant = immutable.tenant,
        localization = MutableLocalization(immutable.localization),
        activities = immutable.activities.map(::MutableActivity).toMutableList(),
        timeSlotRules = immutable.timeSlotRules.toMutableMap(),
        reminders = MutableReminders(immutable.reminders),
        periodicPlanning = MutablePeriodicPlanning(immutable.periodicPlanning),
        sessionLimit = immutable.sessionLimit,
    )

    override fun getActivity(activityId: ActivityId): MutableActivity =
        activities.first { it.id == activityId }

    fun addNewActivity(): MutableActivity {
        val id = ActivityId((activities.maxOfOrNull { it.id.value } ?: 0) + 1)
        val activity = MutableActivity.createNew(id)
        activities.add(activity)
        return activity
    }
}
