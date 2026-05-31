package com.github.shelgen.timesage.repositories

import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.shelgen.timesage.logger
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.io.File
import java.time.Duration
import java.time.Instant

class CachedJsonFileDao<T>(private val jsonClass: Class<T>) {
    private val cache: LoadingCache<String, T?> = Caffeine.newBuilder()
        .build(CacheLoader(::loadFile))

    private val objectMapper: JsonMapper = jacksonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    @Synchronized
    fun save(file: File, json: T) {
        saveFile(file, json)
        cache.invalidate(file.absolutePath)
    }

    @Synchronized
    fun delete(file: File) {
        file.delete()
        cache.invalidate(file.absolutePath)
    }

    fun load(file: File): T? =
        cache.get(file.absolutePath)

    private fun saveFile(file: File, json: T) {
        val start = Instant.now()
        val file = file.also { it.parentFile.mkdirs() }
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, json)
        logger.debug("Saved file ${file.path} in ${Duration.between(start, Instant.now()).toMillis()}ms")
    }

    private fun loadFile(string: String): T? {
        val file = File(string)
        if (!file.exists()) {
            logger.info("File ${file.path} does not yet exist")
            return null
        }

        val start = Instant.now()
        val json = objectMapper.readValue(file, jsonClass)
        logger.debug("Loaded file ${file.path} in ${Duration.between(start, Instant.now()).toMillis()}ms")
        return json
    }

}
