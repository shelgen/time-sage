package com.github.shelgen.timesage.ui

import java.time.Instant

object DiscordFormatter {
    fun bold(text: String) = "**$text**"
    fun italics(text: String) = "*$text*"
    fun quote(text: String) = text.split("\n").joinToString(separator = "\n", postfix = "\n") { "> $it" }
    fun strikethrough(text: String) = "~~${text}~~"
    fun underline(text: String) = "__${text}__"
    fun timestamp(instant: Instant, format: TimestampFormat) =
        "<t:${instant.epochSecond}:${format.formatValue}>"

    fun mentionUser(id: Long) = "<@$id>"
    fun mentionChannel(id: Long) = "<#$id>"

    enum class TimestampFormat(val formatValue: String) {
        RELATIVE_TIME("R"), // 48 seconds ago
        LONG_DATE("D"), // August 9, 2025
        SHORT_DATE("d"), // 8/9/25
        LONG_TIME("T"), // 1:52:00 PM
        SHORT_TIME("t"), // 1:52 PM
        LONG_DATE_TIME("F"), // Saturday, August 9, 2025 at 1:52 PM
        SHORT_DATE_TIME("f") // August 9, 2025 at 1:52 PM
    }
}
