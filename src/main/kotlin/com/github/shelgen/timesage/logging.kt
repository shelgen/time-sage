package com.github.shelgen.timesage

import com.github.shelgen.timesage.domain.OperationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

inline val <reified T> T.logger: Logger
    get() = LoggerFactory.getLogger(T::class.java)

const val MDC_GUILD_ID = "guildId"
const val MDC_CHANNEL_ID = "channelId"
const val MDC_USER_NAME = "userName"


fun withContextMDC(
    context: OperationContext,
    block: (OperationContext) -> Unit
) {
    MDC.putCloseable(MDC_GUILD_ID, context.guildId.toString()).use {
        MDC.putCloseable(MDC_CHANNEL_ID, context.channelId.toString()).use {
            block(context)
        }
    }
}

fun withContextAndUserMDC(
    context: OperationContext,
    userName: String,
    block: (OperationContext) -> Unit
) {
    withContextMDC(context) { context ->
        MDC.putCloseable(MDC_USER_NAME, userName).use {
            block(context)
        }
    }
}
