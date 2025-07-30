package com.github.shelgen.timesage.domain

data class Configuration(
    val guildId: Long,
    val enabled: Boolean,
    val channelId: Long?,
    val activities: List<Activity>,
) {
    fun getActivity(activityId: Int): Activity =
        activities.first { it.id == activityId }
}
