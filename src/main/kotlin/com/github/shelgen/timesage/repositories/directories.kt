package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.Tenant
import java.io.File

val SERVERS_DIR = File("time-sage/servers")

fun getServerDir(guildId: Long): File =
    File(SERVERS_DIR, guildId.toString())

fun getChannelsDir(guildId: Long): File =
    File(getServerDir(guildId), "channels")

fun getTenantDir(tenant: Tenant): File =
    File(getChannelsDir(tenant.guildId), tenant.channelId.toString())

fun getAllTenants(): List<Tenant> =
    SERVERS_DIR.findAllLongSubfolderNames().flatMap { guildId ->
        getChannelsDir(guildId).findAllLongSubfolderNames().map { channelId ->
            Tenant(guildId = guildId, channelId = channelId)
        }
    }

fun File.findAllLongSubfolderNames() =
    takeIf(File::exists)
        ?.listFiles(File::isDirectory).orEmpty()
        .map(File::getName)
        .mapNotNull(String::toLongOrNull)
