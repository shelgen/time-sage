package com.github.shelgen.timesage.time

import com.github.shelgen.timesage.configuration.Localization
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

data class DateRange(
    val fromInclusive: LocalDate,
    val toInclusive: LocalDate
) : ClosedRange<LocalDate> by fromInclusive..toInclusive {
    fun dates(): List<LocalDate> = fromInclusive.datesUntil(toInclusive.plusDays(1)).toList()

    fun serialize(): String = "${fromInclusive}_${toInclusive}"

    override fun toString() = "$fromInclusive through $toInclusive"

    fun toLocalizedString(localization: Localization): String =
        when {
            isANamedMonth() -> fromInclusive.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            isAWeek(localization) -> "week of ${fromInclusive.asShortDate()} through ${toInclusive.asShortDate()}"
            else -> "${fromInclusive.asShortDate()} through ${toInclusive.asShortDate()}"
        }

    private fun LocalDate.asShortDate(): String = format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))

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
