package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
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

sealed class Screen(protected val guildId: Long) : SerializableWithParameters {
    abstract fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent>

    fun render(): MessageCreateData =
        MessageCreateBuilder()
            .useComponentsV2()
            .addComponents(renderComponents(ConfigurationRepository.loadOrInitialize(guildId)))
            .build()

    fun renderEdit(): MessageEditData = fromCreateData(render())
}

sealed class ScreenComponent<SCREEN : Screen, EVENT : Event>(val screen: SCREEN) : SerializableWithParameters {
    abstract fun handle(event: EVENT)

    protected fun GenericComponentInteractionCreateEvent.processAndAddEphemeralScreen(processor: (interactionHook: InteractionHook) -> Screen) {
        deferReply(true).queue { interactionHook ->
            interactionHook.editOriginal(processor(interactionHook).renderEdit()).queue()
        }
    }

    protected fun GenericComponentInteractionCreateEvent.processAndAddPublicScreen(processor: (interactionHook: InteractionHook) -> Screen) {
        deferReply().queue { interactionHook ->
            interactionHook.editOriginal(processor(interactionHook).renderEdit()).queue()
        }
    }

    protected fun GenericComponentInteractionCreateEvent.processAndNavigateTo(processor: (interactionHook: InteractionHook) -> Screen) {
        deferEdit().queue { interactionHook ->
            interactionHook.editOriginal(processor(interactionHook).renderEdit()).queue()
        }
    }

    protected fun GenericComponentInteractionCreateEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        deferEdit().queue { interactionHook ->
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
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
        deferEdit().queue { interactionHook ->
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
        }
    }
}
