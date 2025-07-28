package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.repositories.updateConfiguration
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent

class ConfigurationMainScreen : Screen() {
    override fun renderComponents(): List<MessageTopLevelComponent> =
        ConfigurationRepository.load()
            .let { configuration ->
                listOf(
                    TextDisplay.of("# Time Sage configuration"),
                    Section.of(
                        if (configuration.enabled) {
                            Buttons.Disable(this@ConfigurationMainScreen).render()
                        } else {
                            Buttons.Enable(this@ConfigurationMainScreen).render()
                        },
                        TextDisplay.of(DiscordFormatter.bold("Enabled") + ": " + (if (configuration.enabled) "Yes" else "No"))
                    ),
                    TextDisplay.of(DiscordFormatter.bold("Channel") + ":"),
                    ActionRow.of(
                        SelectMenus.Channel(this@ConfigurationMainScreen).render()
                    ),
                    TextDisplay.of(DiscordFormatter.bold("Campaigns") + ":"),
                ) + if (configuration.campaigns.isEmpty()) {
                    listOf(TextDisplay.of(DiscordFormatter.italics("There are currently no campaigns.")))
                } else {
                    configuration.campaigns.map {
                        Section.of(
                            Buttons.EditCampaign(it.id, this@ConfigurationMainScreen)
                                .render(),
                            TextDisplay.of("- ${it.name}")
                        )
                    }
                } + listOf(
                    ActionRow.of(
                        Buttons.AddCampaign(this@ConfigurationMainScreen).render()
                    )
                )
            }

    override fun parameters(): List<String> = emptyList()

    companion object {
        fun reconstruct(parameters: List<String>) = ConfigurationMainScreen()
    }

    class Buttons {
        class Enable(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            style = ButtonStyle.PRIMARY,
            label = "Enable",
            screen = screen
        ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    updateConfiguration { it.copy(enabled = true) }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Enable>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Enable::class
                ) {
                override fun reconstruct(screenParameters: List<String>, componentParameters: List<String>) =
                    Enable(screen = reconstruct(screenParameters))
            }
        }

        class Disable(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            style = ButtonStyle.PRIMARY,
            label = "Disable",
            screen = screen
        ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    updateConfiguration { it.copy(enabled = false) }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Disable>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Disable::class
                ) {
                override fun reconstruct(screenParameters: List<String>, componentParameters: List<String>) =
                    Disable(screen = reconstruct(screenParameters))
            }
        }

        class EditCampaign(private val campaignId: Int, screen: ConfigurationMainScreen) :
            ScreenButton<ConfigurationMainScreen>(
                style = ButtonStyle.PRIMARY,
                label = "Edit...",
                screen = screen
            ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationCampaignScreen(campaignId) }
            }

            override fun parameters(): List<String> = listOf(campaignId.toString())

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, EditCampaign>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = EditCampaign::class
                ) {
                override fun reconstruct(screenParameters: List<String>, componentParameters: List<String>) =
                    EditCampaign(
                        campaignId = componentParameters.first().toInt(),
                        screen = reconstruct(screenParameters)
                    )
            }
        }

        class AddCampaign(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            style = ButtonStyle.SUCCESS,
            label = "Add new campaign",
            screen = screen
        ) {
            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { interactionHook ->
                    val updatedConfiguration = updateConfiguration { configuration ->
                        val campaignId = (configuration.campaigns.maxOfOrNull { it.id } ?: 0) + 1
                        val newCampaign = ConfigurationRepository.ConfigurationDto.CampaignDto(
                            id = campaignId,
                            name = "New Campaign",
                            gmDiscordIds = sortedSetOf(interactionHook.interaction.user.idLong),
                            playerDiscordIds = sortedSetOf(),
                            maxNumMissingPlayers = 0
                        )
                        configuration.copy(campaigns = (configuration.campaigns + newCampaign).toSortedSet())
                    }
                    ConfigurationCampaignScreen(updatedConfiguration.campaigns.maxOf { it.id })
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, AddCampaign>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = AddCampaign::class
                ) {
                override fun reconstruct(screenParameters: List<String>, componentParameters: List<String>) =
                    AddCampaign(screen = reconstruct(screenParameters))
            }
        }
    }

    class SelectMenus {
        class Channel(screen: ConfigurationMainScreen) : ScreenChannelSelectMenu<ConfigurationMainScreen>(
            minValues = 0,
            maxValues = 1,
            channelTypes = setOf(ChannelType.TEXT),
            defaultSelectedChannelIds = ConfigurationRepository.load().channelId?.let(::listOf).orEmpty(),
            screen = screen
        ) {
            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val newChannelId = event.mentions.channels.firstOrNull()?.idLong
                    updateConfiguration { it.copy(channelId = newChannelId) }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Channel>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Channel::class
                ) {
                override fun reconstruct(screenParameters: List<String>, componentParameters: List<String>) =
                    Channel(screen = reconstruct(screenParameters))
            }
        }
    }
}
