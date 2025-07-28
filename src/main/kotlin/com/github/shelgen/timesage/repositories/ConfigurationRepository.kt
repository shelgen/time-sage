package com.github.shelgen.timesage.repositories

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.shelgen.timesage.logger
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*

object ConfigurationRepository {
    private val cache: LoadingCache<Unit, ConfigurationDto> = Caffeine.newBuilder()
        .build(CacheLoader { _ -> loadFile() })

    fun save(configuration: ConfigurationDto) {
        val start = Instant.now()
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(File(FILE_NAME), configuration)
        cache.invalidate(Unit)
        logger.debug("Saved configuration to $FILE_NAME in ${Duration.between(start, Instant.now()).toMillis()}ms")
    }

    fun load(): ConfigurationDto = cache.get(Unit)

    private fun loadFile(): ConfigurationDto {
        val start = Instant.now()
        return File(FILE_NAME)
            .takeIf(File::exists)
            ?.let { file ->
                objectMapper
                    .readValue<ConfigurationDto>(file)
                    .also {
                        logger.debug(
                            "Loaded configuration from $FILE_NAME in " +
                                    "${Duration.between(start, Instant.now()).toMillis()}ms"
                        )
                    }
            }
            ?: ConfigurationDto(
                enabled = false,
                channelId = null,
                campaigns = sortedSetOf()
            ).also { logger.debug("Created new configuration") }
    }

    data class ConfigurationDto(
        val enabled: Boolean,
        val channelId: Long?,
        val campaigns: SortedSet<CampaignDto>,
    ) {
        data class CampaignDto(
            val id: Int,
            val name: String,
            val gmDiscordIds: SortedSet<Long>,
            val playerDiscordIds: SortedSet<Long>,
            val maxNumMissingPlayers: Int
        ) : Comparable<CampaignDto> {
            override fun compareTo(other: CampaignDto) = id.compareTo(other.id)
        }
    }

    private const val FILE_NAME = "time-sage-configuration.json"
}
