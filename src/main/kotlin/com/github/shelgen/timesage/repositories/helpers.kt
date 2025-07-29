package com.github.shelgen.timesage.repositories

import java.time.LocalDate

fun ConfigurationRepository.ConfigurationDto.getCampaign(campaignId: Int): ConfigurationRepository.ConfigurationDto.CampaignDto =
    campaigns.first { it.id == campaignId }

fun ConfigurationRepository.ConfigurationDto.CampaignDto.getParticipants() =
    (gmDiscordIds + playerDiscordIds).distinct().sorted()

fun updateConfiguration(
    reducer: (configuration: ConfigurationRepository.ConfigurationDto) -> ConfigurationRepository.ConfigurationDto
) = ConfigurationRepository.load()
    .let { reducer(it) }
    .also(ConfigurationRepository::save)

fun updateCampaign(
    campaignId: Int,
    reducer: (campaign: ConfigurationRepository.ConfigurationDto.CampaignDto) -> ConfigurationRepository.ConfigurationDto.CampaignDto
) {
    updateConfiguration { configuration ->
        val oldCampaign = configuration.campaigns.first { it.id == campaignId }
        val newCampaign = reducer(oldCampaign)
        configuration.copy(campaigns = (configuration.campaigns - oldCampaign + newCampaign).toSortedSet())
    }
}

fun updateWeek(
    weekMondayDate: LocalDate,
    reducer: (week: WeekRepository.WeekDto) -> WeekRepository.WeekDto
) {
    WeekRepository.load(weekMondayDate)
        .let { reducer(it) }
        .let(WeekRepository::save)
}

fun <K, V> Map<K, V>.withReplacement(key: K, valueProvider: (V?) -> V): Map<K, V> =
    this.minus(key).plus(key to valueProvider(this[key]))
