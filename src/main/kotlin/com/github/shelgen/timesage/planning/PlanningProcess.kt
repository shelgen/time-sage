package com.github.shelgen.timesage.planning

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.configuration.Activity
import com.github.shelgen.timesage.configuration.Configuration
import com.github.shelgen.timesage.configuration.Member
import com.github.shelgen.timesage.discord.DiscordUserId
import com.github.shelgen.timesage.plan.Plan
import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.time.TimeSlot

open class PlanningProcess(
    val dateRange: DateRange,
    val tenant: Tenant,
    open val state: State,
    open val availabilityInterface: AvailabilityInterface,
    open val availabilityResponses: Map<DiscordUserId, AvailabilityResponse>,
    open val sentReminders: List<SentReminder>,
    open val conclusion: Conclusion?,
    open val planAlternatives: List<Plan>,
) {
    enum class State {
        COLLECTING_AVAILABILITIES, LOCKED, CONCLUDED
    }

    fun usersThatHaventAnswered(configuration: Configuration): List<DiscordUserId> =
        configuration.activities
            .asSequence()
            .flatMap(Activity::members)
            .map(Member::user)
            .filter { availabilityResponses[it] == null }
            .distinct()
            .sortedBy(DiscordUserId::id)
            .toList()

    companion object {
        fun new(dateRange: DateRange, tenant: Tenant, availabilityInterface: AvailabilityInterface): PlanningProcess =
            PlanningProcess(
                dateRange = dateRange,
                tenant = tenant,
                state = State.COLLECTING_AVAILABILITIES,
                availabilityInterface = availabilityInterface,
                availabilityResponses = emptyMap(),
                sentReminders = emptyList(),
                conclusion = null,
                planAlternatives = emptyList()
            )
    }
}

class MutablePlanningProcess(
    tenant: Tenant,
    dateRange: DateRange,
    override var state: State,
    override val availabilityInterface: AvailabilityInterface,
    override val availabilityResponses: MutableMap<DiscordUserId, MutableAvailabilityResponse>,
    override val sentReminders: MutableList<SentReminder>,
    override var conclusion: Conclusion?,
    override var planAlternatives: List<Plan>,
) : PlanningProcess(dateRange, tenant, state, availabilityInterface, availabilityResponses, sentReminders, conclusion, planAlternatives) {
    constructor(immutable: PlanningProcess) : this(
        tenant = immutable.tenant,
        dateRange = immutable.dateRange,
        state = immutable.state,
        availabilityInterface = immutable.availabilityInterface,
        availabilityResponses = immutable.availabilityResponses
            .map { it.key to MutableAvailabilityResponse(it.value) }
            .toMap()
            .toMutableMap(),
        sentReminders = immutable.sentReminders.toMutableList(),
        conclusion = immutable.conclusion,
        planAlternatives = immutable.planAlternatives,
    )

    fun setAvailability(user: DiscordUserId, timeSlot: TimeSlot, availability: Availability) {
        response(user)[timeSlot] = availability
    }

    fun setSessionLimit(user: DiscordUserId, sessionLimit: Int) {
        response(user).sessionLimit = sessionLimit
    }

    private fun response(user: DiscordUserId): MutableAvailabilityResponse =
        availabilityResponses.getOrPut(user) { MutableAvailabilityResponse.createNew() }
}
