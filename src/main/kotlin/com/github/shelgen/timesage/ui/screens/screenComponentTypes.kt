package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.repositories.ConfigurationRepository
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import net.dv8tion.jda.api.utils.messages.MessageEditData.fromCreateData

sealed class Screen(protected val guildId: Long) : SerializableWithParameters {
    protected val configuration = ConfigurationRepository.load(guildId)

    abstract fun renderComponents(): List<MessageTopLevelComponent>

    fun render(): MessageCreateData =
        MessageCreateBuilder()
            .useComponentsV2()
            .addComponents(renderComponents())
            .build()

    fun renderEdit(): MessageEditData = fromCreateData(render())
}

sealed class ScreenComponent<SCREEN : Screen, EVENT : Event, RENDERED>(
    val screen: SCREEN
) : SerializableWithParameters {
    abstract fun handle(event: EVENT)
    abstract fun render(): RENDERED

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

sealed class ScreenButton<SCREEN : Screen>(
    val style: ButtonStyle,
    val label: String?,
    val emoji: Emoji? = null,
    screen: SCREEN
) : ScreenComponent<SCREEN, ButtonInteractionEvent, Button>(screen) {
    override fun render() =
        Button.of(
            style,
            CustomIdSerialization.serialize(this),
            label,
            emoji
        )
}

sealed class ScreenStringSelectMenu<SCREEN : Screen>(
    private val minValues: Int,
    private val maxValues: Int,
    private val options: Collection<String>,
    private val defaultSelectedValues: Collection<String>,
    screen: SCREEN
) : ScreenComponent<SCREEN, StringSelectInteractionEvent, StringSelectMenu>(screen) {
    override fun render() =
        StringSelectMenu.create(CustomIdSerialization.serialize(this))
            .setMinValues(minValues)
            .setMaxValues(maxValues)
            .addOptions(options.map { SelectOption.of(it, it) })
            .setDefaultValues(defaultSelectedValues)
            .build()
}

sealed class ScreenEntitySelectMenu<SCREEN : Screen>(screen: SCREEN) :
    ScreenComponent<SCREEN, EntitySelectInteractionEvent, EntitySelectMenu>(screen)

sealed class ScreenUserSelectMenu<SCREEN : Screen>(
    private val minValues: Int,
    private val maxValues: Int,
    private val defaultSelectedUserIds: Collection<Long>,
    screen: SCREEN
) : ScreenEntitySelectMenu<SCREEN>(screen) {
    override fun render() =
        EntitySelectMenu.create(
            CustomIdSerialization.serialize(this),
            EntitySelectMenu.SelectTarget.USER
        )
            .setMinValues(minValues)
            .setMaxValues(maxValues)
            .setDefaultValues(defaultSelectedUserIds.map(EntitySelectMenu.DefaultValue::user)).build()
}

sealed class ScreenChannelSelectMenu<SCREEN : Screen>(
    private val minValues: Int,
    private val maxValues: Int,
    private val channelTypes: Collection<ChannelType>,
    private val defaultSelectedChannelIds: Collection<Long>,
    screen: SCREEN
) : ScreenEntitySelectMenu<SCREEN>(screen) {
    override fun render() =
        EntitySelectMenu.create(
            CustomIdSerialization.serialize(this),
            EntitySelectMenu.SelectTarget.CHANNEL
        )
            .setMinValues(minValues)
            .setMaxValues(maxValues)
            .setChannelTypes(channelTypes)
            .setDefaultValues(defaultSelectedChannelIds.map(EntitySelectMenu.DefaultValue::channel)).build()
}

sealed class ScreenModal<SCREEN : Screen>(
    val title: String,
    val textInputs: List<TextInput>,
    screen: SCREEN
) : ScreenComponent<SCREEN, ModalInteractionEvent, Modal>(screen) {
    override fun render(): Modal =
        Modal.create(CustomIdSerialization.serialize(this), title)
            .addComponents(textInputs.map { ActionRow.of(it) })
            .build()

    protected fun ModalInteractionEvent.processAndRerender(processor: (interactionHook: InteractionHook) -> Unit) {
        deferEdit().queue { interactionHook ->
            processor(interactionHook)
            interactionHook.editOriginal(screen.renderEdit()).queue()
        }
    }
}
