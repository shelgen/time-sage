package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.discord.DiscordServerId
import com.github.shelgen.timesage.discord.DiscordTextChannelId
import java.io.File

val SERVERS_DIR = File("time-sage/servers")

fun getServerDir(server: DiscordServerId): File =
    File(SERVERS_DIR, server.id.toString())

fun getChannelsDir(server: DiscordServerId): File =
    File(getServerDir(server), "channels")

fun getTenantDir(tenant: Tenant): File =
    File(getChannelsDir(tenant.server), tenant.textChannel.id.toString())

fun getAllTenants(): List<Tenant> =
    SERVERS_DIR.findAllLongSubfolderNames().map(::DiscordServerId).flatMap { server ->
        getChannelsDir(server).findAllLongSubfolderNames().map(::DiscordTextChannelId).map { channel ->
            Tenant(server, channel)
        }
    }

fun File.findAllLongSubfolderNames() =
    takeIf(File::exists)
        ?.listFiles(File::isDirectory).orEmpty()
        .map(File::getName)
        .mapNotNull(String::toLongOrNull)
