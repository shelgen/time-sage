package com.github.shelgen.timesage.repositories

import com.github.shelgen.timesage.domain.OperationContext
import java.io.File

fun getChannelDir(context: OperationContext): File =
    File(getChannelsDir(context.guildId), context.channelId.toString())

fun getChannelsDir(guildId: Long): File =
    File(getServerDir(guildId), "channels")

fun getServerDir(guildId: Long): File =
    File(SERVERS_DIR, guildId.toString())

val SERVERS_DIR = File("time-sage/servers")

fun findAllChannelIds(guildId: Long): List<Long> = getChannelsDir(guildId).findAllLongSubfolderNames()

fun findAllGuildIds(): List<Long> = SERVERS_DIR.findAllLongSubfolderNames()

fun File.findAllLongSubfolderNames() =
    takeIf(File::exists)
        ?.listFiles { channelDir -> channelDir.isDirectory && channelDir.name.toLongOrNull() != null }
        .orEmpty()
        .map(File::getName)
        .map(String::toLong)
