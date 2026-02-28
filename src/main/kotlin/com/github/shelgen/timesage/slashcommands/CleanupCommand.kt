package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.logger
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction
import org.slf4j.MDC
import java.util.concurrent.CompletableFuture

object CleanupCommand : AbstractSlashCommand(
    name = "tscleanup",
    description = "Delete old bot messages, keeping availability and plan messages",
) {
    override fun handle(event: SlashCommandInteractionEvent, context: OperationContext) {
        val outerMdc = MDC.getCopyOfContextMap()
        val selfId = event.jda.selfUser.idLong
        event.deferReply(true).queue { hook ->
            MDC.setContextMap(outerMdc)
            val channel = event.channel.asTextChannel()
            val keepMessageIds = AvailabilitiesWeekRepository.loadAll(context)
                .flatMap { listOfNotNull(it.messageId, it.conclusionMessageId) }

            channel.iterableHistory.forEachAsync { message ->
                if (message.author.idLong == selfId) {
                    if (message.idLong in keepMessageIds) {
                        logger.info("Skipping message ${message.id} as it's an availability or conclusion message")
                    } else {
                        logger.info("Deleting message ${message.id}")
                        message.delete().complete()
                    }
                } else {
                    logger.info("Ignoring message ${message.id} from a diffrent user")
                }
                true
            }

            hook.sendMessage("Queued deletion").queue()
            MDC.clear()
        }
    }
}
