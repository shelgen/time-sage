package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.Tenant
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

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
        val enabled: Boolean,
        val timeZone: TimeZone?,
        val scheduling: Scheduling,
        val activities: List<Activity>,
        val voiceChannelId: Long? = null
    ) {
        data class Activity(
            val id: Int,
            val name: String,
            val members: Members,
            val maxMissingOptionalMembers: Int
        )

        data class Members(
            val required: Set<Long>,
            val optional: Set<Long>
        )

        data class Scheduling(
            val type: Type,
            val startDayOfWeek: DayOfWeek,
            val timeSlotRulesPerDay: TimeSlotRules,
            val daysInAdvanceToStartPlanning: Int,
            val startHourOfDay: Int,
            val reminderIntervalDays: Int,
        ) {
            enum class Type { WEEKLY, MONTHLY }
        }

        data class TimeSlotRules(
            val mondays: LocalTime? = null,
            val tuesdays: LocalTime? = null,
            val wednesdays: LocalTime? = null,
            val thursdays: LocalTime? = null,
            val fridays: LocalTime? = null,
            val saturdays: LocalTime? = null,
            val sundays: LocalTime? = null,
        )
    }

    companion object {
        private const val CONFIGURATION_FILE_NAME = "configuration.json"
    }
}
