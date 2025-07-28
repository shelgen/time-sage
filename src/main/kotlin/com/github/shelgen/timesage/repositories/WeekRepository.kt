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
    private val cache: LoadingCache<LocalDate, WeekDto> = Caffeine.newBuilder()
        .build(CacheLoader(::loadFile))

    fun save(week: WeekDto) {
        val start = Instant.now()
        val file = File(File("time-sage-weeks").also(File::mkdirs), "${week.mondayDate}.json")
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, week)
        cache.invalidate(week.mondayDate)
        logger.debug("Saved week to ${file.path} in ${Duration.between(start, Instant.now()).toMillis()}ms")
    }

    fun load(weekMondayDate: LocalDate): WeekDto = cache.get(weekMondayDate)

    private fun loadFile(weekMondayDate: LocalDate): WeekDto {
        val start = Instant.now()
        val file = File(File("time-sage-weeks").also(File::mkdirs), "$weekMondayDate.json")
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
                mondayDate = weekMondayDate,
                weekAvailabilityMessageDiscordId = null,
                playerResponses = emptyMap()
            ).also { logger.debug("Created new week for monday date $weekMondayDate") }
    }

    data class WeekDto(
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
}
