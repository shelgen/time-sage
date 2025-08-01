package com.github.shelgen.timesage.domain

import java.time.DayOfWeek

data class Configuration(
    val enabled: Boolean,
    val scheduling: Scheduling,
    val activities: List<Activity>,
) {
    data class Scheduling(
        val type: SchedulingType,
        val startDayOfWeek: DayOfWeek
    ) {
        companion object {
            val DEFAULT = Scheduling(type = SchedulingType.WEEKLY, startDayOfWeek = DayOfWeek.MONDAY)
        }
    }

    enum class SchedulingType { WEEKLY }

    fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }

    companion object {
        val DEFAULT = Configuration(enabled = false, scheduling = Scheduling.DEFAULT, activities = emptyList())
    }
}
