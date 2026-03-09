package com.github.shelgen.timesage

import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.discord.DiscordThreadChannelId
import com.github.shelgen.timesage.planning.AvailabilityInterface
import com.github.shelgen.timesage.planning.AvailabilityThread
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
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

    fun getGuild(tenant: Tenant): Guild =
        jda.getGuildById(tenant.server.id)
            ?: error("Error getting guild for $tenant")

    fun getTextChannel(tenant: Tenant): TextChannel =
        jda.getTextChannelById(tenant.textChannel.id)
            ?: error("Error getting text channel for $tenant")

    fun getThreadChannel(threadChannel: DiscordThreadChannelId): ThreadChannel =
        jda.getThreadChannelById(threadChannel.id)
            ?: error("Error getting thread channel for $threadChannel")

    fun pin(availabilityInterface: AvailabilityInterface, tenant: Tenant) {
        pin(availabilityInterface.messageToPin(), tenant)
    }

    fun pin(message: DiscordMessageId, tenant: Tenant) {
        getTextChannel(tenant).pinMessageById(message.id).queue()
    }

    fun unpin(availabilityInterface: AvailabilityInterface, tenant: Tenant) {
        getTextChannel(tenant).unpinMessageById(availabilityInterface.messageToPin().id).queue()
    }

    fun archiveThread(availabilityInterface: AvailabilityInterface) {
        if (availabilityInterface !is AvailabilityThread) return
        getThreadChannel(availabilityInterface.threadChannel).manager.setArchived(true).queue()
    }

    fun unarchiveThread(availabilityInterface: AvailabilityInterface) {
        if (availabilityInterface !is AvailabilityThread) return
        getThreadChannel(availabilityInterface.threadChannel).manager.setArchived(false).queue()
    }
}
