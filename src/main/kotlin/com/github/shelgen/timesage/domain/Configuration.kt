package com.github.shelgen.timesage.domain

import java.util.TimeZone

open class Configuration(
    open val enabled: Boolean,
    open val timeZone: TimeZone,
    open val scheduling: Scheduling,
    open val activities: List<Activity>
) {
    open fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }

    companion object {
        val DEFAULT = Configuration(
            enabled = false,
            timeZone = TimeZone.getTimeZone("Europe/Berlin"),
            scheduling = Scheduling.DEFAULT,
            activities = emptyList()
        )
    }
}

class MutableConfiguration(
    override var enabled: Boolean,
    override var timeZone: TimeZone,
    override val scheduling: MutableScheduling,
    override val activities: MutableList<MutableActivity>,
) : Configuration(
    enabled = enabled,
    timeZone = timeZone,
    scheduling = scheduling,
    activities = activities
) {
    constructor(configuration: Configuration) : this(
        enabled = configuration.enabled,
        timeZone = configuration.timeZone,
        scheduling = MutableScheduling(configuration.scheduling),
        activities = configuration.activities.map(::MutableActivity).toMutableList()
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
