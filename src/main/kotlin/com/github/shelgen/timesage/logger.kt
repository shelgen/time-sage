package com.github.shelgen.timesage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T> T.logger: Logger
    get() = LoggerFactory.getLogger(T::class.java)

const val MDC_GUILD_ID = "guildId"
const val MDC_USER_NAME = "userName"
