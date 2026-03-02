package com.github.shelgen.timesage

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

fun LocalDate.formatAsShortDate(): String = format(DateTimeFormatter.ofPattern("LLLL d", Locale.US))
