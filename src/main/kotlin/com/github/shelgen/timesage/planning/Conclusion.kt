package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.discord.DiscordMessageId
import com.github.shelgen.timesage.plan.PlanId

data class Conclusion(
    val message: DiscordMessageId,
    val plan: PlanId,
)
