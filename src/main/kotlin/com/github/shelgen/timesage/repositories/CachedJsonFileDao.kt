package com.github.shelgen.timesage.repositories

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.shelgen.timesage.logger
import java.io.File
import java.time.Duration
import java.time.Instant

class CachedJsonFileDao<T>(private val jsonClass: Class<T>) {
    private val cache: LoadingCache<String, T?> = Caffeine.newBuilder()
        .build(CacheLoader(::loadFile))

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun save(file: File, json: T) {
        saveFile(file, json)
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
