@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.droidcon.global

import com.droidcon.global.domain.model.Session
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formats API timestamps (ISO-8601 UTC) for human-readable schedule UI in the device timezone.
 */
private fun parseInstant(raw: String): Instant? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    return runCatching { Instant.parse(t) }.getOrNull()
}

private fun toLocalDateTime(instant: Instant): LocalDateTime =
    instant.toLocalDateTime(TimeZone.currentSystemDefault())

private fun pad2(n: Int): String = n.toString().padStart(2, '0')

private fun formatHm(dt: LocalDateTime): String = "${pad2(dt.hour)}:${pad2(dt.minute)}"

private fun formatDdMmYyyy(d: kotlinx.datetime.LocalDate): String =
    "${pad2(d.dayOfMonth)}/${pad2(d.monthNumber)}/${d.year}"

private fun englishWeekdayShort(day: DayOfWeek): String =
    when (day) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }

private fun englishMonthShort(monthNumber: Int): String =
    listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )[monthNumber - 1]

private fun formatEnglishWeekdayDate(d: kotlinx.datetime.LocalDate): String {
    val wd = englishWeekdayShort(d.dayOfWeek)
    val m = englishMonthShort(d.monthNumber)
    return "$wd ${d.dayOfMonth} $m ${d.year}"
}

/** List column: each string is its own line (date / time never merged on one line for cross-day). */
fun formatSessionTimeRangeForListLines(session: Session): List<String> {
    val si = parseInstant(session.startTime) ?: return rawTimeRangeFallbackLines(session)
    val ei = parseInstant(session.endTime) ?: return rawTimeRangeFallbackLines(session)
    val start = toLocalDateTime(si)
    val end = toLocalDateTime(ei)
    return if (start.date == end.date) {
        listOf(
            formatDdMmYyyy(start.date),
            "${formatHm(start)} – ${formatHm(end)}"
        )
    } else {
        listOf(
            formatDdMmYyyy(start.date),
            formatHm(start),
            formatDdMmYyyy(end.date),
            formatHm(end)
        )
    }
}

/** Detail screen: weekday, date, and times in local timezone. */
fun formatSessionTimeRangeForDetail(session: Session): String {
    val si = parseInstant(session.startTime) ?: return rawTimeRangeFallback(session)
    val ei = parseInstant(session.endTime) ?: return rawTimeRangeFallback(session)
    val start = toLocalDateTime(si)
    val end = toLocalDateTime(ei)
    val datePart = formatEnglishWeekdayDate(start.date)
    val times = "${formatHm(start)} – ${formatHm(end)}"
    return if (start.date == end.date) {
        "$datePart · $times"
    } else {
        val endDatePart = formatEnglishWeekdayDate(end.date)
        "$datePart, $times — ends: $endDatePart"
    }
}

private fun rawTimeRangeFallback(session: Session): String {
    val start = session.startTime.trim()
    val end = session.endTime.trim()
    return when {
        start.isBlank() && end.isBlank() -> ""
        start.isBlank() -> end
        end.isBlank() -> start
        else -> "$start – $end"
    }
}

private fun rawTimeRangeFallbackLines(session: Session): List<String> {
    val single = rawTimeRangeFallback(session)
    return if (single.isBlank()) emptyList() else listOf(single)
}
