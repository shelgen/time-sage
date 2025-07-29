package com.github.shelgen.timesage.repositories

import java.io.File

fun getServerDir(guildId: Long): File =
    File(SERVERS_DIR, guildId.toString())

val SERVERS_DIR = File("time-sage/servers")
