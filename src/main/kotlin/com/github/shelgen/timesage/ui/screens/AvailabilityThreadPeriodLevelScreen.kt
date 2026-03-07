package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.Configuration
import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant

class AvailabilityThreadPeriodLevelScreen(targetPeriod: TargetPeriod, tenant: Tenant) : AbstractTargetPeriodScreen(targetPeriod, tenant) {
    override fun renderComponents(configuration: Configuration) =
        PeriodLevelScreenContent<AvailabilityThreadPeriodLevelScreen>(this) {
            Buttons.ToggleSessionLimit(this)
        }.renderComponents(configuration)

    class Buttons {
        class ToggleSessionLimit(override val screen: AvailabilityThreadPeriodLevelScreen) :
            PeriodLevelScreenContent.ToggleSessionLimitButton<AvailabilityThreadPeriodLevelScreen>(screen)
    }
}
