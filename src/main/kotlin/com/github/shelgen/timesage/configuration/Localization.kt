package com.github.shelgen.timesage.configuration

import com.github.shelgen.timesage.time.DateRange
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

open class Localization(
    open val timeZone: TimeZone,
    open val startDayOfWeek: DayOfWeek,
) {
    fun dateOf(instant: Instant): LocalDate = instant.atZone(timeZone.toZoneId()).toLocalDate()

    fun currentDate(): LocalDate = dateOf(Instant.now())

    fun hourOf(instant: Instant): Int = instant.atZone(timeZone.toZoneId()).hour

    fun currentHour(): Int = hourOf(Instant.now())

    fun weekOf(date: LocalDate): DateRange = DateRange.weekFrom(startOfWeekOf(date))

    fun currentWeek(): DateRange = weekOf(currentDate())

    fun nextWeek(): DateRange = weekOf(currentDate().plusWeeks(1))

    fun currentMonth(): DateRange = DateRange.from(YearMonth.from(currentDate()))

    fun nextMonth(): DateRange = DateRange.from(YearMonth.from(currentDate().plusMonths(1)))

    private fun startOfWeekOf(date: LocalDate): LocalDate {
        val daysBack = (DayOfWeek.entries.size + date.dayOfWeek.value - startDayOfWeek.value) % DayOfWeek.entries.size
        return date.minusDays(daysBack.toLong())
    }

    companion object {
        val DEFAULT = Localization(
            timeZone = TimeZone.getTimeZone("Europe/Berlin"),
            startDayOfWeek = DayOfWeek.MONDAY,
        )
    }
}

class MutableLocalization(
    override var timeZone: TimeZone,
    override var startDayOfWeek: DayOfWeek,
) : Localization(timeZone, startDayOfWeek) {
    constructor(immutable: Localization) : this(
        timeZone = immutable.timeZone,
        startDayOfWeek = immutable.startDayOfWeek,
    )
}
