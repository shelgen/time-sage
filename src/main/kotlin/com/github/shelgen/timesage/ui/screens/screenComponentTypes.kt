package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import net.dv8tion.jda.api.components.MessageTopLevelComponent
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

sealed class Screen(protected val context: OperationContext) : SerializableWithParameters {
    abstract fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent>

    fun render(): MessageCreateData =
        MessageCreateBuilder()
            .useComponentsV2()
            .addComponents(renderComponents(ConfigurationRepository.loadOrInitialize(context)))
            .build()

    fun renderEdit(): MessageEditData = fromCreateData(render())
}

sealed class ScreenComponent<SCREEN : Screen, EVENT : Event>(val screen: SCREEN) : SerializableWithParameters {
    abstract fun handle(event: EVENT)

    protected fun GenericComponentInteractionCreateEvent.processAndAddEphemeralScreen(processor: (interactionHook: InteractionHook) -> Screen) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferReply(true).queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            val newScreen = processor(interactionHook)
            interactionHook.editOriginal(newScreen.renderEdit()).queue()
            MDC.clear()
        }
    }

    protected fun GenericComponentInteractionCreateEvent.processAndAddPublicScreen(processor: (interactionHook: InteractionHook) -> Screen) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferReply().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            val newScreen = processor(interactionHook)
            interactionHook.editOriginal(newScreen.renderEdit()).queue()
            MDC.clear()
        }
    }

    protected fun GenericComponentInteractionCreateEvent.processAndNavigateTo(processor: (interactionHook: InteractionHook) -> Screen) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            val newScreen = processor(interactionHook)
            interactionHook.editOriginal(newScreen.renderEdit()).queue()
            MDC.clear()
        }
    }

    protected fun GenericComponentInteractionCreateEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
            MDC.clear()
        }
    }
}

sealed class ScreenButton<SCREEN : Screen>(screen: SCREEN) :
    ScreenComponent<SCREEN, ButtonInteractionEvent>(screen)

sealed class ScreenStringSelectMenu<SCREEN : Screen>(screen: SCREEN) :
    ScreenComponent<SCREEN, StringSelectInteractionEvent>(screen)

sealed class ScreenEntitySelectMenu<SCREEN : Screen>(screen: SCREEN) :
    ScreenComponent<SCREEN, EntitySelectInteractionEvent>(screen)

sealed class ScreenModal<SCREEN : Screen>(screen: SCREEN) :
    ScreenComponent<SCREEN, ModalInteractionEvent>(screen) {
    protected fun ModalInteractionEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        val outerMdc = MDC.getCopyOfContextMap()
        deferEdit().queue { interactionHook ->
            MDC.setContextMap(outerMdc)
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
            MDC.clear()
        }
    }
}
