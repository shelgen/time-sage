package com.github.shelgen.timesage

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.io.File

object JDAHolder {
    val jda: JDA = run {
        val botToken = File("time-sage-bot.token").readText().trim()
        logger.info("Logging on with token")
        val build = JDABuilder.createDefault(botToken)
            .setActivity(Activity.customStatus("Being developed"))
            .build()
        logger.info("Waiting for JDA to be ready")
        build.awaitReady()
    }
}

fun replaceBotPinsWith(message: Message) {
    val channel = message.channel.asTextChannel()
    val botPinnedMessages = mutableListOf<Message>()
    channel.retrievePinnedMessages().forEachAsync { pinned ->
        if (pinned.message.author.idLong == JDAHolder.jda.selfUser.idLong) {
            botPinnedMessages.add(pinned.message)
        }
        true
    }.thenRun { unpinSequentiallyThenPin(botPinnedMessages, channel, message.idLong) }
}

private fun unpinSequentiallyThenPin(
    remaining: List<Message>,
    channel: TextChannel,
    newMessageId: Long
) {
    if (remaining.isEmpty()) {
        channel.pinMessageById(newMessageId).queue()
    } else {
        remaining.first().unpin().queue {
            unpinSequentiallyThenPin(remaining.drop(1), channel, newMessageId)
        }
    }
}
