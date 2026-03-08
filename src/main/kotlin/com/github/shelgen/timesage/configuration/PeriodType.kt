package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.time.DateRange
import java.time.YearMonth

enum class PeriodType {
    WEEKLY {
        override fun nextPeriod(localization: Localization): DateRange {
            val aWeekFromNow = localization.currentDate().plusWeeks(1)
            return localization.weekOf(aWeekFromNow)
        }
    },
    MONTHLY {
        override fun nextPeriod(localization: Localization): DateRange {
            val aMonthFromNow = localization.currentDate().plusMonths(1)
            return DateRange.from(YearMonth.from(aMonthFromNow))
        }
    };

    abstract fun nextPeriod(localization: Localization): DateRange
}
