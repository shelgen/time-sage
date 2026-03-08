package com.github.shelgen.timesage

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

inline val <reified T> T.logger: Logger
    get() = LoggerFactory.getLogger(T::class.java)

const val MDC_GUILD_ID = "guildId"
const val MDC_CHANNEL_ID = "channelId"
const val MDC_USER_NAME = "userName"

fun withTenantMDC(tenant: Tenant, block: (Tenant) -> Unit) {
    MDC.putCloseable(MDC_GUILD_ID, tenant.server.toString()).use {
        MDC.putCloseable(MDC_CHANNEL_ID, tenant.channel.toString()).use {
            block(tenant)
        }
    }
}

fun withTenantAndUserMDC(tenant: Tenant, userName: String, block: (Tenant) -> Unit) {
    withTenantMDC(tenant) { tenant ->
        MDC.putCloseable(MDC_USER_NAME, userName).use {
            block(tenant)
        }
    }
}
