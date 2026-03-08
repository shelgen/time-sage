package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.time.DateRange

sealed class AbstractDateRangeScreen(val dateRange: DateRange, tenant: Tenant) : Screen(tenant)
