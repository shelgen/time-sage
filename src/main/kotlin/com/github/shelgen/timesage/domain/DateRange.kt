package com.github.shelgen.timesage.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

typealias TargetPeriod = DateRange

data class DateRange(
    val fromInclusive: LocalDate,
    val toInclusive: LocalDate
) {
    fun dates(): List<LocalDate> = fromInclusive.datesUntil(toInclusive.plusDays(1)).toList()

    override fun toString() = "$fromInclusive through $toInclusive"

    fun toLocalizedString(localization: Localization) =
        when {
            isANamedMonth() -> fromInclusive.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            isAWeek(localization) -> "week of $fromInclusive through $toInclusive"
            else -> "$fromInclusive through $toInclusive"
        }

    fun toUnit(localization: Localization) =
        when {
            isANamedMonth() -> "month"
            isAWeek(localization) -> "week"
            else -> "period"
        }

    /**
     * Splits the dates of this date range into week chunks, each starting on [startDayOfWeek].
     * The first and last chunk may be partial weeks.
     */
    fun chunkedByWeek(startDayOfWeek: DayOfWeek): List<List<LocalDate>> {
        val result = mutableListOf<MutableList<LocalDate>>()
        var current = mutableListOf<LocalDate>()
        for (date in dates()) {
            if (date.dayOfWeek == startDayOfWeek && current.isNotEmpty()) {
                result.add(current)
                current = mutableListOf()
            }
            current.add(date)
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }

    private fun isANamedMonth(): Boolean {
        val yearMonth = YearMonth.from(fromInclusive)
        return yearMonth.atDay(1) == fromInclusive && yearMonth.atEndOfMonth() == toInclusive
    }

    private fun isAWeek(localization: Localization): Boolean =
        fromInclusive.dayOfWeek == localization.startDayOfWeek && toInclusive == fromInclusive.plusDays(6)

    companion object {
        fun weekFrom(startDate: LocalDate) =
            DateRange(startDate, startDate.plusDays(6))

        fun from(yearMonth: YearMonth) =
            DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth())
    }
}
