package com.github.shelgen.timesage.repositories

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.shelgen.timesage.logger
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

object WeekRepository {
    private val cache: LoadingCache<CacheKey, WeekDto> = Caffeine.newBuilder()
        .build(CacheLoader { loadFile(guildId = it.guildId, weekMondayDate = it.weekMondayDate) })

    data class CacheKey(val guildId: Long, val weekMondayDate: LocalDate)

    fun save(week: WeekDto) {
        val start = Instant.now()
        val file = getWeekFile(guildId = week.guildId, weekMondayDate = week.mondayDate).also { it.parentFile.mkdirs() }
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, week)
        cache.invalidate(CacheKey(guildId = week.guildId, weekMondayDate = week.mondayDate))
        logger.debug("Saved week to ${file.path} in ${Duration.between(start, Instant.now()).toMillis()}ms")
    }

    fun load(guildId: Long, weekMondayDate: LocalDate): WeekDto = cache.get(
        CacheKey(
            guildId = guildId,
            weekMondayDate = weekMondayDate
        )
    )

    private fun loadFile(guildId: Long, weekMondayDate: LocalDate): WeekDto {
        val start = Instant.now()
        val file = getWeekFile(guildId = guildId, weekMondayDate = weekMondayDate)
        return file
            .takeIf(File::exists)
            ?.let { file ->
                objectMapper
                    .readValue<WeekDto>(file)
                    .also {
                        logger.debug(
                            "Loaded week from ${file.path} in " +
                                    "${Duration.between(start, Instant.now()).toMillis()}ms"
                        )
                    }
            }
            ?: WeekDto(
                guildId = guildId,
                mondayDate = weekMondayDate,
                weekAvailabilityMessageDiscordId = null,
                playerResponses = emptyMap()
            ).also { logger.debug("Created new week for monday date $weekMondayDate") }
    }

    data class WeekDto(
        val guildId: Long,
        val mondayDate: LocalDate,
        val weekAvailabilityMessageDiscordId: Long?,
        val playerResponses: Map<Long, PlayerResponse>,
    ) {
        data class PlayerResponse(
            val sessionLimit: Int?,
            val availability: Map<LocalDate, AvailabilityStatus>
        ) {
            enum class AvailabilityStatus {
                AVAILABLE, IF_NEED_BE, UNAVAILABLE
            }
        }
    }

    private fun getWeekFile(guildId: Long, weekMondayDate: LocalDate): File =
        File(getWeekDir(guildId), "$weekMondayDate.json")

    private fun getWeekDir(guildId: Long): File =
        File(getServerDir(guildId), "weeks")
}
