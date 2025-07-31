package com.github.shelgen.timesage.domain

data class Configuration(
    val enabled: Boolean,
    val activities: List<Activity>,
) {
    fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }
}
