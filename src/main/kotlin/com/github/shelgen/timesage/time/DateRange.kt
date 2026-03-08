package com.github.shelgen.timesage.time

import com.github.shelgen.timesage.configuration.Localization
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

data class DateRange(
    val fromInclusive: LocalDate,
    val toInclusive: LocalDate
): ClosedRange<LocalDate> by fromInclusive..toInclusive {
    fun dates(): List<LocalDate> = fromInclusive.datesUntil(toInclusive.plusDays(1)).toList()

    fun serialize(): String = "${fromInclusive}_${toInclusive}"

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
    fun chunkedByWeek(startDayOfWeek: DayOfWeek): List<DateRange> {
        val result = mutableListOf<DateRange>()
        var chunkStart: LocalDate? = null
        var chunkEnd: LocalDate? = null
        for (date in dates()) {
            if (date.dayOfWeek == startDayOfWeek && chunkStart != null) {
                result.add(DateRange(chunkStart, chunkEnd!!))
                chunkStart = null
            }
            if (chunkStart == null) chunkStart = date
            chunkEnd = date
        }
        if (chunkStart != null) result.add(DateRange(chunkStart, chunkEnd!!))
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

        fun deserialize(serialized: String): DateRange {
            val (from, to) = serialized.split("_", limit = 2)
            return DateRange(LocalDate.parse(from), LocalDate.parse(to))
        }
    }
}
