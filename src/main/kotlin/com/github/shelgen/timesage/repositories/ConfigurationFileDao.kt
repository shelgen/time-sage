package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.Tenant
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime

class ConfigurationFileDao {
    private val fileDao = CachedJsonFileDao<Json>(jsonClass = Json::class.java)

    fun save(tenant: Tenant, json: Json) {
        fileDao.save(getConfigurationFile(tenant), json)
    }

    fun load(tenant: Tenant): Json? =
        fileDao.load(getConfigurationFile(tenant))

    private fun getConfigurationFile(tenant: Tenant): File =
        File(getTenantDir(tenant), CONFIGURATION_FILE_NAME)

    data class Json(
        val tenant: Tenant,
        val localization: Localization,
        val activities: List<Activity>,
        val timeSlotRules: Map<DayOfWeek, LocalTime>,
        val reminders: Reminders,
        val periodicPlanning: PeriodicPlanning,
    ) {
        data class Tenant(
            val serverId: Long,
            val textChannelId: Long,
        )

        data class Localization(
            val timeZone: String,
            val startDayOfWeek: DayOfWeek,
        )

        data class Activity(
            val id: Int,
            val name: String,
            val members: List<Member>,
            val maxNumMissingOptionalMembers: Int,
            val voiceChannelId: Long?,
        )

        data class Member(
            val userId: Long,
            val optional: Boolean,
        )

        data class Reminders(
            val enabled: Boolean,
            val intervalDays: Int,
            val hourOfDay: Int,
        )

        data class PeriodicPlanning(
            val enabled: Boolean,
            val interval: Interval,
            val daysInAdvance: Int,
            val hourOfDay: Int,
        )

        enum class Interval { WEEKLY, MONTHLY }
    }

    companion object {
        private const val CONFIGURATION_FILE_NAME = "configuration.json"
    }
}
