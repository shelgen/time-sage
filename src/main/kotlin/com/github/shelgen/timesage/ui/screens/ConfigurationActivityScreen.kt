package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.OperationContext
import com.github.shelgen.timesage.domain.Participant
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.modals.Modal

class ConfigurationActivityScreen(private val activityId: Int, context: OperationContext) : Screen(context) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        listOf(
            TextDisplay.of(
                "# Time Sage configuration\n" +
                        "## Activity settings"
            ),
            Section.of(
                Buttons.EditName(this).render(),
                TextDisplay.of("### ${configuration.getActivity(activityId).name}")
            ),
            TextDisplay.of(DiscordFormatter.bold("Required participants") + ":"),
            ActionRow.of(
                SelectMenus.RequiredParticipants(this).render(configuration)
            ),
            TextDisplay.of(DiscordFormatter.bold("Optional participants") + ":"),
            ActionRow.of(
                SelectMenus.OptionalParticipants(this).render(configuration)
            ),
            TextDisplay.of(DiscordFormatter.bold("Maximum number of missing optional participants") + ":"),
            ActionRow.of(
                SelectMenus.MaxMissingOptionalParticipants(this).render(configuration)
            ),
            ActionRow.of(
                Buttons.Back(this).render()
            )
        )

    class Buttons {
        class EditName(override val screen: ConfigurationActivityScreen) : ScreenButton {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit name...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.context)
                val modal = Modals.EditName(screen).render(configuration)
                event.replyModal(modal).queue()
            }
        }

        class Back(override val screen: ConfigurationActivityScreen) : ScreenButton {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationMainScreen(screen.context) }
            }
        }
    }

    class SelectMenus {
        class RequiredParticipants(override val screen: ConfigurationActivityScreen) : ScreenEntitySelectMenu {
            fun render(configuration: Configuration) =
                EntitySelectMenu
                    .create(CustomIdSerialization.serialize(this), SelectTarget.USER)
                    .setMinValues(1)
                    .setMaxValues(25)
                    .setDefaultValues(
                        configuration.getActivity(screen.activityId)
                            .participants
                            .filterNot(Participant::optional)
                            .map(Participant::userId)
                            .map(EntitySelectMenu.DefaultValue::user)
                    ).build()

            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val updatedUsers = event.mentions.users.map { it.idLong }
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        val activity = configuration.getActivity(activityId = screen.activityId)
                        activity.setRequiredParticipants(updatedUsers)
                    }
                }
            }
        }

        class OptionalParticipants(override val screen: ConfigurationActivityScreen) : ScreenEntitySelectMenu {
            fun render(configuration: Configuration) =
                EntitySelectMenu
                    .create(CustomIdSerialization.serialize(this), SelectTarget.USER)
                    .setMinValues(0)
                    .setMaxValues(25)
                    .setDefaultValues(
                        configuration.getActivity(screen.activityId)
                            .participants
                            .filter(Participant::optional)
                            .map(Participant::userId)
                            .map(EntitySelectMenu.DefaultValue::user)
                    ).build()

            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val updatedUsers = event.mentions.users.map { it.idLong }
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        val activity = configuration.getActivity(activityId = screen.activityId)
                        activity.setOptionalParticipants(updatedUsers)
                    }
                }
            }
        }

        class MaxMissingOptionalParticipants(override val screen: ConfigurationActivityScreen) :
            ScreenStringSelectMenu {
            fun render(configuration: Configuration) =
                StringSelectMenu.create(CustomIdSerialization.serialize(this))
                    .setMinValues(1)
                    .setMaxValues(1)
                    .addOptions((0..4).map(Int::toString).map { SelectOption.of(it, it) })
                    .setDefaultValues(configuration.getActivity(screen.activityId).maxMissingOptionalParticipants.toString())
                    .build()

            override fun handle(event: StringSelectInteractionEvent) {
                event.processAndRerender {
                    val updatedMaxMissingOptionalParticipants = event.values.first().toInt()
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        val activity = configuration.getActivity(activityId = screen.activityId)
                        activity.maxMissingOptionalParticipants = updatedMaxMissingOptionalParticipants
                    }
                }
            }
        }
    }

    class Modals {
        class EditName(override val screen: ConfigurationActivityScreen) : ScreenModal {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit name for activity")
                    .addComponents(
                        Label.of(
                            "Name",
                            TextInput.create("name", TextInputStyle.SHORT)
                                .setPlaceholder("Name of the activity")
                                .setValue(configuration.getActivity(screen.activityId).name)
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    val updatedName = event.getValue("name")!!.asString
                    ConfigurationRepository.update(context = screen.context) { configuration ->
                        val activity = configuration.getActivity(activityId = screen.activityId)
                        activity.name = updatedName
                    }
                }
            }
        }
    }
}
