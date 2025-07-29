package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.repositories.getCampaign
import com.github.shelgen.timesage.repositories.updateCampaign
import com.github.shelgen.timesage.repositories.updateConfiguration
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

class ConfigurationCampaignScreen(private val campaignId: Int, guildId: Long) : Screen(guildId) {
    override fun renderComponents(): List<MessageTopLevelComponent> =
        listOf(
            TextDisplay.of(
                "# Time Sage configuration\n" +
                        "## Campaign settings"
            ),
            Section.of(
                Buttons.EditName(this@ConfigurationCampaignScreen).render(),
                TextDisplay.of("### ${configuration.getCampaign(campaignId).name}")
            ),
            TextDisplay.of(DiscordFormatter.bold("GM's") + ":"),
            ActionRow.of(
                SelectMenus.GMs(this@ConfigurationCampaignScreen).render()
            ),
            TextDisplay.of(DiscordFormatter.bold("Players") + ":"),
            ActionRow.of(
                SelectMenus.Players(this@ConfigurationCampaignScreen).render()
            ),
            TextDisplay.of(DiscordFormatter.bold("Maximum number of missing players") + ":"),
            ActionRow.of(
                SelectMenus.MaxMissingPlayers(this@ConfigurationCampaignScreen).render()
            ),
            ActionRow.of(
                Buttons.Delete(this@ConfigurationCampaignScreen).render()
            ),
            ActionRow.of(
                Buttons.Back(this@ConfigurationCampaignScreen).render()
            )
        )

    override fun parameters(): List<String> = listOf(campaignId.toString())

    companion object {
        fun reconstruct(parameters: List<String>, guildId: Long) =
            ConfigurationCampaignScreen(campaignId = parameters.first().toInt(), guildId)
    }

    class Buttons {
        class EditName(screen: ConfigurationCampaignScreen) : ScreenButton<ConfigurationCampaignScreen>(
            style = ButtonStyle.PRIMARY,
            label = "Edit name...",
            screen = screen
        ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.replyModal(Modals.EditName(screen).render()).queue()
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
            style = ButtonStyle.DANGER,
            label = "Delete campaign",
            screen = screen
        ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo {
                    updateConfiguration(screen.guildId) { configuration ->
                        val campaignToRemove = configuration.campaigns.first { it.id == screen.campaignId }
                        configuration.copy(campaigns = (configuration.campaigns - campaignToRemove).toSortedSet())
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
            style = ButtonStyle.SECONDARY,
            label = "Back",
            screen = screen
        ) {
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
        class GMs(screen: ConfigurationCampaignScreen) : ScreenUserSelectMenu<ConfigurationCampaignScreen>(
            minValues = 1,
            maxValues = 25,
            defaultSelectedUserIds = screen.configuration.getCampaign(screen.campaignId).gmDiscordIds,
            screen = screen
        ) {
            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val updatedUsers = event.mentions.users.map { it.idLong }
                    updateCampaign(guildId = screen.guildId, campaignId = screen.campaignId) {
                        it.copy(gmDiscordIds = updatedUsers.toSortedSet())
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
            ScreenUserSelectMenu<ConfigurationCampaignScreen>(
                minValues = 1,
                maxValues = 25,
                defaultSelectedUserIds = screen.configuration.getCampaign(screen.campaignId).playerDiscordIds,
                screen = screen
            ) {
            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val updatedUsers = event.mentions.users.map { it.idLong }
                    updateCampaign(guildId = screen.guildId, campaignId = screen.campaignId) {
                        it.copy(playerDiscordIds = updatedUsers.toSortedSet())
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
            ScreenStringSelectMenu<ConfigurationCampaignScreen>(
                minValues = 1,
                maxValues = 1,
                options = (0..4).map(Int::toString),
                defaultSelectedValues = listOf(screen.configuration.getCampaign(screen.campaignId).maxNumMissingPlayers.toString()),
                screen = screen
            ) {
            override fun handle(event: StringSelectInteractionEvent) {
                event.processAndRerender {
                    val updatedMaxNumMissingPlayers = event.values.first().toInt()
                    updateCampaign(guildId = screen.guildId, campaignId = screen.campaignId) {
                        it.copy(maxNumMissingPlayers = updatedMaxNumMissingPlayers)
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
        class EditName(screen: ConfigurationCampaignScreen) : ScreenModal<ConfigurationCampaignScreen>(
            title = "Edit name for campaign",
            textInputs = listOf(
                TextInput.create("name", "Name", TextInputStyle.SHORT)
                    .setPlaceholder("Name of the campaign")
                    .setValue(screen.configuration.getCampaign(screen.campaignId).name)
                    .build()
            ),
            screen = screen
        ) {
            override fun handle(event: ModalInteractionEvent) {
                event.processAndRerender {
                    val updatedName = event.getValue("name")!!.asString
                    updateCampaign(guildId = screen.guildId, campaignId = screen.campaignId) {
                        it.copy(name = updatedName)
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

