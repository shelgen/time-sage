package com.github.shelgen.timesage.repositories

import java.io.File
import java.util.SortedSet

class ConfigurationFileDao {
    private val fileDao = CachedJsonFileDao<Json>(
        jsonClass = Json::class.java,
        initialContent = Json(
            enabled = false,
            channelId = null,
            campaigns = sortedSetOf()
        )
    )

    fun save(guildId: Long, json: Json) {
        fileDao.save(getConfigurationFile(guildId), json)
    }

    fun loadOrInitialize(guildId: Long): Json =
        fileDao.loadOrInitialize(getConfigurationFile(guildId))

    fun findAllGuildIds(): List<Long> =
        SERVERS_DIR.listFiles { serverDir ->
            serverDir.isDirectory &&
                    serverDir.name.toLongOrNull() != null &&
                    serverDir.listFiles { it.isFile && it.name == CONFIGURATION_FILE_NAME }.orEmpty().isNotEmpty()
        }.orEmpty()
            .map(File::getName)
            .map(String::toLong)

    private fun getConfigurationFile(guildId: Long): File =
        File(getServerDir(guildId), CONFIGURATION_FILE_NAME)

    data class Json(
        val enabled: Boolean,
        val channelId: Long?,
        val campaigns: SortedSet<Campaign>,
    ) {
        data class Campaign(
            val id: Int,
            val name: String,
            val gmDiscordIds: SortedSet<Long>,
            val playerDiscordIds: SortedSet<Long>,
            val maxNumMissingPlayers: Int
        ) : Comparable<Campaign> {
            override fun compareTo(other: Campaign) = id.compareTo(other.id)
        }
    }

    companion object {
        private const val CONFIGURATION_FILE_NAME = "configuration.json"
    }
}
