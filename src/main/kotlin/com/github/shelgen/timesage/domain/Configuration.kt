package com.github.shelgen.timesage.domain

open class Configuration(
    open val enabled: Boolean,
    open val scheduling: Scheduling,
    open val activities: List<Activity>
) {
    open fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }

    companion object {
        val DEFAULT = Configuration(enabled = false, scheduling = Scheduling.DEFAULT, activities = emptyList())
    }
}

class MutableConfiguration(
    override var enabled: Boolean,
    override val scheduling: MutableScheduling,
    override val activities: MutableList<MutableActivity>,
) : Configuration(
    enabled = enabled,
    scheduling = scheduling,
    activities = activities
) {
    constructor(configuration: Configuration) : this(
        enabled = configuration.enabled,
        scheduling = MutableScheduling(configuration.scheduling),
        activities = configuration.activities.map(::MutableActivity).toMutableList()
    )

    override fun getActivity(activityId: Int): MutableActivity =
        activities.first { it.id == activityId }

    fun addNewActivity(): Int {
        val id = (activities.maxOfOrNull { it.id } ?: 0) + 1
        val activity = MutableActivity.createNew(id)
        activities.add(activity)
        return id
    }
}
