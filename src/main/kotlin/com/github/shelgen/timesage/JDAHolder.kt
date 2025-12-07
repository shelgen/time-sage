package com.github.shelgen.timesage

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import java.io.File
import java.util.TimeZone

object JDAHolder {
    val jda: JDA = run {
        val botToken = File("time-sage-bot.token").readText().trim()
        logger.info("Logging on with token")
        val build = JDABuilder.createDefault(botToken)
            .setActivity(Activity.customStatus("Being developed"))
            .build()
        logger.info("Waiting for JDA to be ready")
        logger.info(
            TimeZone.getAvailableIDs()
                .groupBy { it.substringBefore('/') }.entries.joinToString("\n") { it.key + ": " + it.value.size })
        logger.info("${TimeZone.getAvailableIDs().size} total zones")
        build.awaitReady()
    }
}
