package com.github.shelgen.timesage.domain

data class Configuration(
    val guildId: Long,
    val enabled: Boolean,
    val channelId: Long?,
    val campaigns: Set<Campaign>,
) {
    fun getCampaign(campaignId: Int): Campaign =
        campaigns.first { it.id == campaignId }

    data class Campaign(
        val id: Int,
        val name: String,
        val gmDiscordIds: Set<Long>,
        val playerDiscordIds: Set<Long>,
        val maxNumMissingPlayers: Int
    ) {
        fun getParticipants() =
            (gmDiscordIds + playerDiscordIds).distinct().sorted()
    }
}
