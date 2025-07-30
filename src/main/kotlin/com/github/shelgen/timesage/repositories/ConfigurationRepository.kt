package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.Configuration

object ConfigurationRepository {
    private val dao = ConfigurationFileDao()

    fun loadOrInitialize(guildId: Long): Configuration = dao.loadOrInitialize(guildId).toConfiguration(guildId)

    fun <T> update(
        guildId: Long,
        modification: (configuration: MutableConfiguration) -> T
    ): T {
        val configuration = loadOrInitialize(guildId)
        val mutableConfiguration = MutableConfiguration(configuration)
        val returnValue = modification(mutableConfiguration)
        dao.save(guildId, mutableConfiguration.toJson())
        return returnValue
    }

    fun findAllGuildIds() = dao.findAllGuildIds()

    private fun ConfigurationFileDao.Json.toConfiguration(guildId: Long): Configuration =
        Configuration(
            guildId = guildId,
            enabled = enabled,
            channelId = channelId,
            campaigns = campaigns.map { it.toCampaign() }.toMutableSet(),
        )

    private fun ConfigurationFileDao.Json.Campaign.toCampaign(): Configuration.Campaign =
        Configuration.Campaign(
            id = this.id,
            name = this.name,
            gmDiscordIds = this.gmDiscordIds.toMutableSet(),
            playerDiscordIds = this.playerDiscordIds.toMutableSet(),
            maxNumMissingPlayers = this.maxNumMissingPlayers
        )

    data class MutableConfiguration(
        val guildId: Long,
        var enabled: Boolean,
        var channelId: Long?,
        val campaigns: MutableSet<MutableCampaign>
    ) {
        constructor(configuration: Configuration) : this(
            guildId = configuration.guildId,
            enabled = configuration.enabled,
            channelId = configuration.channelId,
            campaigns = configuration.campaigns.map(::MutableCampaign).toMutableSet()
        )

        fun getCampaign(campaignId: Int): MutableCampaign =
            campaigns.first { it.id == campaignId }

        data class MutableCampaign(
            val id: Int,
            var name: String,
            val gmDiscordIds: MutableSet<Long>,
            val playerDiscordIds: MutableSet<Long>,
            var maxNumMissingPlayers: Int

        ) {
            constructor(campaign: Configuration.Campaign) : this(
                id = campaign.id,
                name = campaign.name,
                gmDiscordIds = campaign.gmDiscordIds.toMutableSet(),
                playerDiscordIds = campaign.playerDiscordIds.toMutableSet(),
                maxNumMissingPlayers = campaign.maxNumMissingPlayers
            )

            fun toJson(): ConfigurationFileDao.Json.Campaign =
                ConfigurationFileDao.Json.Campaign(
                    id = id,
                    name = name,
                    gmDiscordIds = gmDiscordIds.toSortedSet(),
                    playerDiscordIds = playerDiscordIds.toSortedSet(),
                    maxNumMissingPlayers = maxNumMissingPlayers
                )
        }

        fun toJson(): ConfigurationFileDao.Json =
            ConfigurationFileDao.Json(
                enabled = enabled,
                channelId = channelId,
                campaigns = campaigns.map { it.toJson() }.toSortedSet(),
            )
    }
}
