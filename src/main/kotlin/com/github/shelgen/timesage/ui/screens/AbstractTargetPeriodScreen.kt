package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.TargetPeriod
import com.github.shelgen.timesage.domain.Tenant

abstract class AbstractTargetPeriodScreen(val targetPeriod: TargetPeriod, tenant: Tenant) : Screen(tenant)
