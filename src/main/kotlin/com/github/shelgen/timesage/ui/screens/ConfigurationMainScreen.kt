package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.cronjobs.AvailabilityMessageSender
import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.repositories.ConfigurationRepository
import com.github.shelgen.timesage.ui.DiscordFormatter
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent

class ConfigurationMainScreen(guildId: Long) : Screen(guildId) {
    override fun renderComponents(configuration: Configuration): List<MessageTopLevelComponent> =
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
                SelectMenus.Channel(this@ConfigurationMainScreen).render(configuration)
            ),
            TextDisplay.of(DiscordFormatter.bold("Campaigns") + ":"),
        ) + if (configuration.campaigns.isEmpty()) {
            listOf(TextDisplay.of(DiscordFormatter.italics("There are currently no campaigns.")))
        } else {
            configuration.campaigns.map {
                Section.of(
                    Buttons.EditCampaign(it.id, this@ConfigurationMainScreen).render(),
                    TextDisplay.of("- ${it.name}")
                )
            }
        } + listOf(
            ActionRow.of(
                Buttons.AddCampaign(this@ConfigurationMainScreen).render()
            )
        )

    override fun parameters(): List<String> = emptyList()

    companion object {
        fun reconstruct(parameters: List<String>, guildId: Long) = ConfigurationMainScreen(guildId)
    }

    class Buttons {
        class Enable(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Enable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.guildId) { it.enabled = true }
                    AvailabilityMessageSender.postAvailabilityMessage(screen.guildId)
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Enable>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Enable::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    Enable(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }

        class Disable(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Disable")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndRerender {
                    ConfigurationRepository.update(screen.guildId) { it.enabled = false }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Disable>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Disable::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    Disable(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }

        class EditCampaign(private val campaignId: Int, screen: ConfigurationMainScreen) :
            ScreenButton<ConfigurationMainScreen>(
                screen = screen
            ) {
            fun render() =
                Button.primary(CustomIdSerialization.serialize(this), "Edit...")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { ConfigurationCampaignScreen(campaignId, screen.guildId) }
            }

            override fun parameters(): List<String> = listOf(campaignId.toString())

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, EditCampaign>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = EditCampaign::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    EditCampaign(
                        campaignId = componentParameters.first().toInt(),
                        screen = reconstruct(parameters = screenParameters, guildId = guildId)
                    )
            }
        }

        class AddCampaign(screen: ConfigurationMainScreen) : ScreenButton<ConfigurationMainScreen>(
            screen = screen
        ) {
            fun render() =
                Button.success(CustomIdSerialization.serialize(this), "Add new campaign")

            override fun handle(event: ButtonInteractionEvent) {
                event.processAndNavigateTo { interactionHook ->
                    val newCampaignId = ConfigurationRepository.update(screen.guildId) { configuration ->
                        ConfigurationRepository.MutableConfiguration.MutableCampaign(
                            id = (configuration.campaigns.maxOfOrNull { it.id } ?: 0) + 1,
                            name = "New Campaign",
                            gmDiscordIds = mutableSetOf(interactionHook.interaction.user.idLong),
                            playerDiscordIds = mutableSetOf(),
                            maxNumMissingPlayers = 0
                        ).also(configuration.campaigns::add).id
                    }
                    ConfigurationCampaignScreen(newCampaignId, screen.guildId)
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, AddCampaign>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = AddCampaign::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    AddCampaign(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }
    }

    class SelectMenus {
        class Channel(screen: ConfigurationMainScreen) : ScreenEntitySelectMenu<ConfigurationMainScreen>(screen) {
            fun render(configuration: Configuration) =
                EntitySelectMenu
                    .create(CustomIdSerialization.serialize(this), EntitySelectMenu.SelectTarget.CHANNEL)
                    .setMinValues(0)
                    .setMaxValues(1)
                    .setChannelTypes(ChannelType.TEXT)
                    .setDefaultValues(listOfNotNull(configuration.channelId?.let(EntitySelectMenu.DefaultValue::channel)))
                    .build()

            override fun handle(event: EntitySelectInteractionEvent) {
                event.processAndRerender {
                    val newChannelId = event.mentions.channels.firstOrNull()?.idLong
                    ConfigurationRepository.update(screen.guildId) { it.channelId = newChannelId }
                }
            }

            override fun parameters(): List<String> = emptyList()

            object Reconstructor :
                ScreenComponentReconstructor<ConfigurationMainScreen, Channel>(
                    screenClass = ConfigurationMainScreen::class,
                    componentClass = Channel::class
                ) {
                override fun reconstruct(
                    screenParameters: List<String>,
                    componentParameters: List<String>,
                    guildId: Long
                ) =
                    Channel(screen = reconstruct(parameters = screenParameters, guildId = guildId))
            }
        }
    }
}
