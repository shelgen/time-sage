package com.github.shelgen.timesage.ui

import java.time.OffsetDateTime

object DiscordFormatter {
    fun bold(text: String) = "**$text**"
    fun italics(text: String) = "*$text*"
    fun quote(text: String) = text.split("\n").joinToString(separator = "\n", postfix = "\n") { "> $it" }
    fun strikethrough(text: String) = "~~${text}~~"
    fun underline(text: String) = "__${text}__"
    fun timestamp(offsetDateTime: OffsetDateTime, format: TimestampFormat) =
        "<t:${offsetDateTime.toEpochSecond()}:${format.formatValue}>"

    fun mentionUser(id: Long) = "<@$id>"
    fun mentionChannel(id: Long) = "<#$id>"

    enum class TimestampFormat(val formatValue: String) {
        RELATIVE_TIME("R"), // 0 seconds ago
        LONG_DATE("D"), // March 5, 2020
        SHORT_DATE("d"), // 05/03/2020
        LONG_TIME("T"), // 11:28:27 AM
        SHORT_TIME("t"), // 11:28 AM
        LONG_DATE_TIME("F"), // Thursday, March 5, 2020 11:28:27 AM
        SHORT_DATE_TIME("f") // 5 March 2020 11:28
    }
}
