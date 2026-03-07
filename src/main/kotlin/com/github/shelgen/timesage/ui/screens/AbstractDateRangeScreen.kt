package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.DateRange
import com.github.shelgen.timesage.domain.Tenant

abstract class AbstractDateRangeScreen(val dateRange: DateRange, tenant: Tenant) : Screen(tenant)
