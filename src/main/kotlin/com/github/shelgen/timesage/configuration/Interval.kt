package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.time.DateRange

enum class Interval {
    WEEKLY {
        override fun nextDateRange(localization: Localization): DateRange = localization.nextWeek()
    },
    MONTHLY {
        override fun nextDateRange(localization: Localization): DateRange = localization.nextMonth()
    };

    abstract fun nextDateRange(localization: Localization): DateRange
}
