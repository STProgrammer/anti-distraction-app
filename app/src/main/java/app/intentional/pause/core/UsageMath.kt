package app.intentional.pause.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

object UsageMath {
    fun split(start: Long, end: Long, zone: ZoneId): Map<LocalDate, Long> {
        if (end <= start) return emptyMap()
        val totals = linkedMapOf<LocalDate, Long>()
        var cursor = start
        while (cursor < end) {
            val day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val boundary = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val until = minOf(end, boundary)
            totals[day] = (totals[day] ?: 0) + until - cursor
            cursor = until
        }
        return totals
    }

    fun clock(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1000
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    }

    fun short(ms: Long): String {
        val minutes = ms.coerceAtLeast(0) / 60000
        return when {
            minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            ms > 0 -> "${ms / 1000}s"
            else -> "0m"
        }
    }
}
