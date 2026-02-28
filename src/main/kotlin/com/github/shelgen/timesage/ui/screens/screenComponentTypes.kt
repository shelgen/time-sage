package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.JDAHolder
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import net.dv8tion.jda.api.utils.messages.MessageEditData.fromCreateData
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private val logger = LoggerFactory.getLogger("ScreenComponentInteraction")

sealed class Screen(val context: OperationContext) {
    abstract fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent>

    fun render(): MessageCreateData = MessageCreateBuilder().useComponentsV2()
        .addComponents(renderComponents(ConfigurationRepository.loadOrInitialize(context))).build()

    fun renderEdit(): MessageEditData = fromCreateData(render())
}

sealed interface ScreenComponent<EVENT : Event> {
    val screen: Screen
    fun handle(event: EVENT)

    fun GenericComponentInteractionCreateEvent.processAndAddEphemeralScreen(processor: (interactionHook: InteractionHook) -> Screen) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferReply(true).queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            try {
                val newScreen = processor(interactionHook)
                interactionHook.editOriginal(newScreen.renderEdit()).queue()
            } catch (e: Exception) {
                logger.error("Error in processAndAddEphemeralScreen (screen=${screen::class.simpleName})", e)
                interactionHook.editOriginal("An error occurred.").queue()
            } finally {
                MDC.clear()
            }
        }
    }

    fun GenericComponentInteractionCreateEvent.processAndAddPublicScreen(
        onMessagePosted: ((Message) -> Unit)? = null,
        processor: (interactionHook: InteractionHook) -> Screen
    ) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferReply().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            try {
                val newScreen = processor(interactionHook)
                interactionHook.editOriginal(newScreen.renderEdit()).queue { message ->
                    MDC.setContextMap(outerMdc)
                    onMessagePosted?.invoke(message)
                    MDC.clear()
                }
            } catch (e: Exception) {
                logger.error("Error in processAndAddPublicScreen (screen=${screen::class.simpleName})", e)
                interactionHook.editOriginal("An error occurred.").queue()
            } finally {
                MDC.clear()
            }
        }
    }

    fun GenericComponentInteractionCreateEvent.processAndNavigateTo(processor: (interactionHook: InteractionHook) -> Screen) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            try {
                val newScreen = processor(interactionHook)
                interactionHook.editOriginal(newScreen.renderEdit()).queue()
            } catch (e: Exception) {
                logger.error("Error in processAndNavigateTo (screen=${screen::class.simpleName})", e)
            } finally {
                MDC.clear()
            }
        }
    }

    fun GenericComponentInteractionCreateEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            try {
                processor(interactionHook)
                interactionHook.editOriginal(screen.renderEdit()).queue()
            } catch (e: Exception) {
                logger.error("Error in processAndRerender (screen=${screen::class.simpleName})", e)
            } finally {
                MDC.clear()
            }
        }
    }

    fun rerenderOtherScreen(messageId: Long, screen: Screen) {
        JDAHolder.jda.getTextChannelById(this.screen.context.channelId)
            ?.editMessageById(messageId, screen.renderEdit())
            ?.queue()
    }
}

sealed interface ScreenButton : ScreenComponent<ButtonInteractionEvent>

sealed interface ScreenStringSelectMenu : ScreenComponent<StringSelectInteractionEvent>

sealed interface ScreenEntitySelectMenu : ScreenComponent<EntitySelectInteractionEvent>

sealed interface ScreenModal : ScreenComponent<ModalInteractionEvent> {
    fun ModalInteractionEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            try {
                processor(interactionHook)
                interactionHook.editOriginal(screen.renderEdit()).queue()
            } catch (e: Exception) {
                logger.error("Error in modal processAndRerender (screen=${screen::class.simpleName})", e)
            } finally {
                MDC.clear()
            }
        }
    }
}
