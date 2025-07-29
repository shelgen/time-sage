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
    private val cache: LoadingCache<Long, ConfigurationDto> = Caffeine.newBuilder()
        .build(CacheLoader(::loadFile))

    fun save(configuration: ConfigurationDto) {
        val start = Instant.now()
        val file = getConfigurationFile(configuration.guildId).also { it.parentFile.mkdirs() }
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, configuration)
        cache.invalidate(configuration.guildId)
        logger.debug("Saved configuration to ${file.path} in ${Duration.between(start, Instant.now()).toMillis()}ms")
    }

    fun load(guildId: Long): ConfigurationDto = cache.get(guildId)

    fun loadAll(): List<ConfigurationDto> =
        SERVERS_DIR.listFiles { serverDir ->
            serverDir.isDirectory &&
                    serverDir.name.toLongOrNull() != null &&
                    serverDir.listFiles { it.isFile && it.name == CONFIGURATION_FILE_NAME }.orEmpty().isNotEmpty()
        }.orEmpty()
            .map(File::getName)
            .map(String::toLong)
            .map(::load)

    private fun loadFile(guildId: Long): ConfigurationDto {
        val start = Instant.now()
        val file = getConfigurationFile(guildId)
        return file
            .takeIf(File::exists)
            ?.let { file ->
                objectMapper
                    .readValue<ConfigurationDto>(file)
                    .also {
                        logger.debug(
                            "Loaded configuration from ${file.path} in " +
                                    "${Duration.between(start, Instant.now()).toMillis()}ms"
                        )
                    }
            }
            ?: ConfigurationDto(
                guildId = guildId,
                enabled = false,
                channelId = null,
                campaigns = sortedSetOf()
            ).also { logger.debug("Created new configuration for guildId $guildId") }
    }

    data class ConfigurationDto(
        val guildId: Long,
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

    private fun getConfigurationFile(guildId: Long): File =
        File(getServerDir(guildId), CONFIGURATION_FILE_NAME)

    private const val CONFIGURATION_FILE_NAME = "configuration.json"
}
