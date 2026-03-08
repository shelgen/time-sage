package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.Tenant
import com.github.shelgen.timesage.time.DateRange

abstract class AbstractDateRangeScreen(val dateRange: DateRange, tenant: Tenant) : Screen(tenant)
