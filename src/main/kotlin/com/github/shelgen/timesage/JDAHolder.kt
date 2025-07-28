package com.github.shelgen.timesage

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import org.slf4j.LoggerFactory
import java.io.File

object JDAHolder {
    val jda: JDA = run {
        val botToken = File("time-sage-bot.token").readText()
        val logger = LoggerFactory.getLogger(javaClass)
        logger.info("Logging on with token")
        val build = JDABuilder.createDefault(botToken)
            .setActivity(Activity.playing(" at being developed"))
            .build()
        logger.info("Waiting for JDA to be ready")
        build.awaitReady()
    }
}
