package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.time.DateRange
import com.github.shelgen.timesage.Tenant

abstract class AbstractDateRangeScreen(val dateRange: DateRange, tenant: Tenant) : Screen(tenant)
