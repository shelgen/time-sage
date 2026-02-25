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
import org.slf4j.MDC

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
            val newScreen = processor(interactionHook)
            interactionHook.editOriginal(newScreen.renderEdit()).queue()
            MDC.clear()
        }
    }

    fun GenericComponentInteractionCreateEvent.processAndAddPublicScreen(
        onMessagePosted: ((Message) -> Unit)? = null,
        processor: (interactionHook: InteractionHook) -> Screen
    ) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferReply().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            val newScreen = processor(interactionHook)
            interactionHook.editOriginal(newScreen.renderEdit()).queue { message ->
                MDC.setContextMap(outerMdc)
                onMessagePosted?.invoke(message)
                MDC.clear()
            }
            MDC.clear()
        }
    }

    fun GenericComponentInteractionCreateEvent.processAndNavigateTo(processor: (interactionHook: InteractionHook) -> Screen) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            val newScreen = processor(interactionHook)
            interactionHook.editOriginal(newScreen.renderEdit()).queue()
            MDC.clear()
        }
    }

    fun GenericComponentInteractionCreateEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
            MDC.clear()
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
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
            MDC.clear()
        }
    }
}
