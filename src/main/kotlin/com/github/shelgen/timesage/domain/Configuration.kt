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
    )

    enum class SchedulingType { WEEKLY }

    fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }
}
