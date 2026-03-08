package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.ActivityId
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Localization
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.configuration.MutableConfiguration
import com.github.shelgen.timesage.configuration.PeriodicPlanning
import com.github.shelgen.timesage.configuration.PeriodType
import com.github.shelgen.timesage.configuration.Reminders
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.discord.DiscordVoiceChannelId
import java.util.TimeZone

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(tenant: Tenant): Configuration =
        dao.load(tenant)
            ?.toDomain()
            ?: Configuration.createDefault(tenant)

    @Synchronized
    fun <T> update(
        tenant: Tenant,
        modification: (configuration: MutableConfiguration) -> T
    ): T {
        val configuration = loadOrInitialize(tenant)
        val mutableConfiguration = MutableConfiguration(configuration)
        val returnValue = modification(mutableConfiguration)
        dao.save(tenant, mutableConfiguration.toJson())
        return returnValue
    }

    fun all(): Sequence<Configuration> = getAllTenants().map { loadOrInitialize(it) }.asSequence()

    private fun ConfigurationFileDao.Json.toDomain(): Configuration = Configuration(
        tenant = tenant.toDomain(),
        localization = localization.toDomain(),
        activities = activities.map { it.toDomain() },
        timeSlotRules = timeSlotRules,
        reminders = reminders.toDomain(),
        periodicPlanning = periodicPlanning.toDomain(),
    )

    private fun ConfigurationFileDao.Json.Tenant.toDomain() = Tenant(
        server = DiscordServerId(serverId),
        textChannel = DiscordTextChannelId(textChannelId),
    )

    private fun ConfigurationFileDao.Json.Localization.toDomain() = Localization(
        timeZone = TimeZone.getTimeZone(timeZone),
        startDayOfWeek = startDayOfWeek,
    )

    private fun ConfigurationFileDao.Json.Activity.toDomain() = Activity(
        id = ActivityId(id),
        name = name,
        members = members.map { it.toDomain() },
        maxNumMissingOptionalMembers = maxNumMissingOptionalMembers,
        voiceChannel = voiceChannelId?.let { DiscordVoiceChannelId(it) },
    )

    private fun ConfigurationFileDao.Json.Member.toDomain() = Member(
        user = DiscordUserId(userId),
        optional = optional,
    )

    private fun ConfigurationFileDao.Json.Reminders.toDomain() = Reminders(
        enabled = enabled,
        intervalDays = intervalDays,
        hourOfDay = hourOfDay,
    )

    private fun ConfigurationFileDao.Json.PeriodicPlanning.toDomain() = PeriodicPlanning(
        enabled = enabled,
        periodType = periodType.toDomain(),
        daysInAdvance = daysInAdvance,
        hourOfDay = hourOfDay,
    )

    private fun ConfigurationFileDao.Json.PeriodType.toDomain() = when (this) {
        ConfigurationFileDao.Json.PeriodType.WEEKLY -> PeriodType.WEEKLY
        ConfigurationFileDao.Json.PeriodType.MONTHLY -> PeriodType.MONTHLY
    }

    private fun Configuration.toJson() = ConfigurationFileDao.Json(
        tenant = tenant.toJson(),
        localization = localization.toJson(),
        activities = activities.sortedBy { it.id.value }.map { it.toJson() },
        timeSlotRules = timeSlotRules,
        reminders = reminders.toJson(),
        periodicPlanning = periodicPlanning.toJson(),
    )

    private fun Tenant.toJson() = ConfigurationFileDao.Json.Tenant(
        serverId = server.id,
        textChannelId = textChannel.id,
    )

    private fun Localization.toJson() = ConfigurationFileDao.Json.Localization(
        timeZone = timeZone.id,
        startDayOfWeek = startDayOfWeek,
    )

    private fun Activity.toJson() = ConfigurationFileDao.Json.Activity(
        id = id.value,
        name = name,
        members = members.map { it.toJson() },
        maxNumMissingOptionalMembers = maxNumMissingOptionalMembers,
        voiceChannelId = voiceChannel?.id,
    )

    private fun Member.toJson() = ConfigurationFileDao.Json.Member(
        userId = user.id,
        optional = optional,
    )

    private fun Reminders.toJson() = ConfigurationFileDao.Json.Reminders(
        enabled = enabled,
        intervalDays = intervalDays,
        hourOfDay = hourOfDay,
    )

    private fun PeriodicPlanning.toJson() = ConfigurationFileDao.Json.PeriodicPlanning(
        enabled = enabled,
        periodType = periodType.toJson(),
        daysInAdvance = daysInAdvance,
        hourOfDay = hourOfDay,
    )

    private fun PeriodType.toJson() = when (this) {
        PeriodType.WEEKLY -> ConfigurationFileDao.Json.PeriodType.WEEKLY
        PeriodType.MONTHLY -> ConfigurationFileDao.Json.PeriodType.MONTHLY
    }
}
