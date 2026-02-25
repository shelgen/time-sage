package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.MDC

object CleanupCommand : AbstractSlashCommand(
    name = "tscleanup",
    description = "Delete old bot messages, keeping availability and plan messages",
) {
    override fun handle(event: SlashCommandInteractionEvent, context: OperationContext) {
        val outerMdc = MDC.getCopyOfContextMap()
        event.deferReply(true).queue {
            MDC.setContextMap(outerMdc)
            val channel = event.channel.asTextChannel()
            val selfId = event.jda.selfUser.idLong
            val keepMessageIds = AvailabilitiesWeekRepository.loadAll(context).flatMap { listOfNotNull(it.messageId, it.conclusionMessageId) }

            var firstMessageId = ""
            channel.iterableHistory
                .forEachAsync { message ->
                    if (message.author.idLong == selfId && message.idLong !in keepMessageIds) {
                        firstMessageId = message.id
                        logger.info("Would delete https://discord.com/channels/${context.guildId}/${context.channelId}/$message")
                        false
                    } else {
                        true
                    }
                }.get()
            it.sendMessage("Would have deleted https://discord.com/channels/${context.guildId}/${context.channelId}/$firstMessageId").queue()
            MDC.clear()
        }
    }
}
