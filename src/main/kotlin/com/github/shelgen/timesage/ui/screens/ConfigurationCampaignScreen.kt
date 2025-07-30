package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
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
import net.dv8tion.jda.api.interactions.modals.Modal

class ConfigurationCampaignScreen(private val campaignId: Int, guildId: Long) : Screen(guildId) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
        listOf(
            TextDisplay.of(
                "# Time Sage configuration\n" +
                        "## Campaign settings"
            ),
            Section.of(
                Buttons.EditName(this).render(),
                TextDisplay.of("### ${configuration.getCampaign(campaignId).name}")
            ),
            TextDisplay.of(DiscordFormatter.bold("GM's") + ":"),
            ActionRow.of(
                SelectMenus.GMs(this).render(configuration)
            ),
            TextDisplay.of(DiscordFormatter.bold("Players") + ":"),
            ActionRow.of(
                SelectMenus.Players(this).render(configuration)
            ),
            TextDisplay.of(DiscordFormatter.bold("Maximum number of missing players") + ":"),
            ActionRow.of(
                SelectMenus.MaxMissingPlayers(this).render(configuration)
            ),
            ActionRow.of(
                Buttons.Delete(this).render()
            ),
            ActionRow.of(
                Buttons.Back(this).render()
            )
        )

    override fun parameters(): List<String> = listOf(campaignId.toString())

    companion object {
        fun reconstruct(parameters: List<String>, guildId: Long) =
            ConfigurationCampaignScreen(campaignId = parameters.first().toInt(), guildId)
    }

    class Buttons {
        class EditName(screen: ConfigurationCampaignScreen) : ScreenButton<ConfigurationCampaignScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit name...")

            override fun handle(event: ButtonInteractionEvent) {
                val configuration = ConfigurationRepository.loadOrInitialize(screen.guildId)
                val modal = Modals.EditName(screen).render(configuration)
                event.replyModal(modal).queue()
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationCampaignScreen, EditName>(
                    screenClass = ConfigurationCampaignScreen::class,
                    componentClass = EditName::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    EditName(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }

        class Delete(screen: ConfigurationCampaignScreen) : ScreenButton<ConfigurationCampaignScreen>(
            screen = screen
        ) {
            fun render() =
                Button.danger(CustomIdSerialization.serialize(this), "Delete campaign")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    ConfigurationRepository.update(screen.guildId) { configuration ->
                        configuration.campaigns.removeIf { it.id == screen.campaignId }
                    }
                    ConfigurationMainScreen(screen.guildId)
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationCampaignScreen, Delete>(
                    screenClass = ConfigurationCampaignScreen::class,
                    componentClass = Delete::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    Delete(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }

        class Back(screen: ConfigurationCampaignScreen) : ScreenButton<ConfigurationCampaignScreen>(
            screen = screen
        ) {
            fun render() =
                Button.secondary(CustomIdSerialization.serialize(this), "Back")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationMainScreen(screen.guildId) }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationCampaignScreen, Back>(
                    screenClass = ConfigurationCampaignScreen::class,
                    componentClass = Back::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    Back(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }
    }

    class SelectMenus {
        class GMs(screen: ConfigurationCampaignScreen) : ScreenEntitySelectMenu<ConfigurationCampaignScreen>(screen) {
            fun render(configuration: Configuration) =
                EntitySelectMenu
                    .create(CustomIdSerialization.serialize(this), SelectTarget.USER)
                    .setMinValues(1)
                    .setMaxValues(25)
                    .setDefaultValues(
                        configuration.getCampaign(screen.campaignId).gmDiscordIds
                            .map(EntitySelectMenu.DefaultValue::user)
                    ).build()

            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val updatedUsers = event.mentions.users.map { it.idLong }
                    ConfigurationRepository.update(guildId = screen.guildId) { configuration ->
                        val campaign = configuration.getCampaign(campaignId = screen.campaignId)
                        campaign.gmDiscordIds.clear()
                        campaign.gmDiscordIds.addAll(updatedUsers)
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor : ScreenComponentReconstructor<ConfigurationCampaignScreen, GMs>(
                screenClass = ConfigurationCampaignScreen::class,
                componentClass = GMs::class
            ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    GMs(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }

        class Players(screen: ConfigurationCampaignScreen) :
            ScreenEntitySelectMenu<ConfigurationCampaignScreen>(screen) {
            fun render(configuration: Configuration) =
                EntitySelectMenu
                    .create(CustomIdSerialization.serialize(this), SelectTarget.USER)
                    .setMinValues(0)
                    .setMaxValues(25)
                    .setDefaultValues(
                        configuration.getCampaign(screen.campaignId).playerDiscordIds
                            .map(EntitySelectMenu.DefaultValue::user)
                    ).build()

            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val updatedUsers = event.mentions.users.map { it.idLong }
                    ConfigurationRepository.update(guildId = screen.guildId) { configuration ->
                        val campaign = configuration.getCampaign(campaignId = screen.campaignId)
                        campaign.playerDiscordIds.clear()
                        campaign.playerDiscordIds.addAll(updatedUsers)
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationCampaignScreen, Players>(
                    screenClass = ConfigurationCampaignScreen::class,
                    componentClass = Players::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    Players(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }

        class MaxMissingPlayers(screen: ConfigurationCampaignScreen) :
            ScreenStringSelectMenu<ConfigurationCampaignScreen>(screen) {
            fun render(configuration: Configuration) =
                StringSelectMenu.create(CustomIdSerialization.serialize(this))
                    .setMinValues(1)
                    .setMaxValues(1)
                    .addOptions((0..4).map(Int::toString).map { SelectOption.of(it, it) })
                    .setDefaultValues(configuration.getCampaign(screen.campaignId).maxNumMissingPlayers.toString())
                    .build()

            override fun handle(event: StringSelectInteractionEvent) {
                event.processAndRerender {
                    val updatedMaxNumMissingPlayers = event.values.first().toInt()
                    ConfigurationRepository.update(guildId = screen.guildId) { configuration ->
                        val campaign = configuration.getCampaign(campaignId = screen.campaignId)
                        campaign.maxNumMissingPlayers = updatedMaxNumMissingPlayers
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor : ScreenComponentReconstructor<ConfigurationCampaignScreen, MaxMissingPlayers>(
                screenClass = ConfigurationCampaignScreen::class,
                componentClass = MaxMissingPlayers::class
            ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    MaxMissingPlayers(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }
    }

    class Modals {
        class EditName(screen: ConfigurationCampaignScreen) : ScreenModal<ConfigurationCampaignScreen>(screen) {
            fun render(configuration: Configuration) =
                Modal
                    .create(CustomIdSerialization.serialize(this), "Edit name for campaign")
                    .addComponents(
                        ActionRow.of(
                            TextInput.create("name", "Name", TextInputStyle.SHORT)
                                .setPlaceholder("Name of the campaign")
                                .setValue(configuration.getCampaign(screen.campaignId).name)
                                .build()
                        )
                    )
                    .build()

            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    val updatedName = event.getValue("name")!!.asString
                    ConfigurationRepository.update(guildId = screen.guildId) { configuration ->
                        val campaign = configuration.getCampaign(campaignId = screen.campaignId)
                        campaign.name = updatedName
                    }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationCampaignScreen, EditName>(
                    screenClass = ConfigurationCampaignScreen::class,
                    componentClass = EditName::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    EditName(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }
    }
}

