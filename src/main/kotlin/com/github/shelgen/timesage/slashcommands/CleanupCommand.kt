package com.github.shelgen.timesage.slashcommands

import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.AvailabilitiesWeekRepository
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private val logger = LoggerFactory.getLogger(CleanupCommand::class.java)

private const val PAGE_SIZE = 50
private const val PROGRESS_INTERVAL_MS = 3000L

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
                .toSet()
            hook.editOriginal("Cleanup started, this may take a while…").queue()
            processPage(
                history = MessageHistory(channel),
                keepMessageIds = keepMessageIds,
                selfId = selfId,
                hook = hook,
                totalRead = 0,
                totalDeleted = 0,
                lastProgressMs = System.currentTimeMillis(),
                mdcContext = outerMdc,
            )
            MDC.clear()
        }
    }
}

private fun processPage(
    history: MessageHistory,
    keepMessageIds: Set<Long>,
    selfId: Long,
    hook: InteractionHook,
    totalRead: Int,
    totalDeleted: Int,
    lastProgressMs: Long,
    mdcContext: Map<String, String>?,
) {
    history.retrievePast(PAGE_SIZE).queue { messages ->
        MDC.setContextMap(mdcContext)
        if (messages.isEmpty()) {
            logger.info("Cleanup complete: read $totalRead messages, deleted $totalDeleted")
            hook.editOriginal("Cleanup complete. Read $totalRead messages, deleted $totalDeleted.").queue()
            MDC.clear()
            return@queue
        }

        val newTotalRead = totalRead + messages.size
        val now = System.currentTimeMillis()
        val newLastProgressMs = if (now - lastProgressMs >= PROGRESS_INTERVAL_MS) {
            hook.editOriginal("Working… read $newTotalRead messages, deleted $totalDeleted so far.").queue()
            now
        } else lastProgressMs

        val toDelete = messages.filter { msg ->
            if (msg.author.idLong != selfId) {
                logger.info("Ignoring message ${msg.id} from another user")
                false
            } else if (msg.idLong in keepMessageIds) {
                logger.info("Keeping message ${msg.id} (availability or conclusion)")
                false
            } else {
                logger.info("Deleting message ${msg.id}")
                true
            }
        }

        deleteSequentially(
            toDelete = toDelete,
            history = history,
            keepMessageIds = keepMessageIds,
            selfId = selfId,
            hook = hook,
            totalRead = newTotalRead,
            totalDeleted = totalDeleted,
            lastProgressMs = newLastProgressMs,
            mdcContext = mdcContext,
        )
        MDC.clear()
    }
}

private fun deleteSequentially(
    toDelete: List<Message>,
    history: MessageHistory,
    keepMessageIds: Set<Long>,
    selfId: Long,
    hook: InteractionHook,
    totalRead: Int,
    totalDeleted: Int,
    lastProgressMs: Long,
    mdcContext: Map<String, String>?,
) {
    if (toDelete.isEmpty()) {
        processPage(history, keepMessageIds, selfId, hook, totalRead, totalDeleted, lastProgressMs, mdcContext)
        return
    }
    toDelete.first().delete().queue {
        MDC.setContextMap(mdcContext)
        val newTotalDeleted = totalDeleted + 1
        val now = System.currentTimeMillis()
        val newLastProgressMs = if (now - lastProgressMs >= PROGRESS_INTERVAL_MS) {
            hook.editOriginal("Working… read $totalRead messages, deleted $newTotalDeleted so far.").queue()
            now
        } else lastProgressMs
        deleteSequentially(
            toDelete = toDelete.drop(1),
            history = history,
            keepMessageIds = keepMessageIds,
            selfId = selfId,
            hook = hook,
            totalRead = totalRead,
            totalDeleted = newTotalDeleted,
            lastProgressMs = newLastProgressMs,
            mdcContext = mdcContext,
        )
        MDC.clear()
    }
}
