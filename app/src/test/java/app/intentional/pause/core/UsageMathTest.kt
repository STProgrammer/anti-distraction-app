package app.intentional.pause.core

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageMathTest {
    private val oslo = ZoneId.of("Europe/Oslo")
    private fun time(value: String) = ZonedDateTime.parse(value).toInstant().toEpochMilli()
    @Test fun combinesIndependentAppDurations() {
        val start = time("2026-10-24T12:00:00+02:00[Europe/Oslo]")
        val pieces = listOf(5, 3, 7).map { UsageMath.split(start, start + it * 60_000L, oslo).values.sum() }
        assertEquals("00:15:00", UsageMath.clock(pieces.sum()))
    }
    @Test fun midnightIsSplitWithoutLosingMilliseconds() {
        val start = time("2026-10-24T23:59:50+02:00[Europe/Oslo]")
        val split = UsageMath.split(start, start + 25_123, oslo)
        assertEquals(10_000L, split[LocalDate.parse("2026-10-24")]); assertEquals(15_123L, split[LocalDate.parse("2026-10-25")])
    }
    @Test fun autumnDstDayContains25Hours() {
        val start = LocalDate.parse("2026-10-25").atStartOfDay(oslo).toInstant().toEpochMilli()
        val end = LocalDate.parse("2026-10-26").atStartOfDay(oslo).toInstant().toEpochMilli()
        assertEquals(25 * 3600_000L, UsageMath.split(start, end, oslo).values.single())
    }
    @Test fun springDstDayContains23Hours() {
        val start = LocalDate.parse("2026-03-29").atStartOfDay(oslo).toInstant().toEpochMilli()
        val end = LocalDate.parse("2026-03-30").atStartOfDay(oslo).toInstant().toEpochMilli()
        assertEquals(23 * 3600_000L, UsageMath.split(start, end, oslo).values.single())
    }
    @Test fun zeroAndBackwardsIntervalsAreIgnored() {
        assertTrue(UsageMath.split(20, 20, oslo).isEmpty()); assertTrue(UsageMath.split(30, 20, oslo).isEmpty())
    }
    @Test fun exactMidnightDoesNotAddAnEmptyNextDay() {
        val start = time("2026-10-24T23:00:00+02:00[Europe/Oslo]")
        assertEquals(1, UsageMath.split(start, start + 3600_000, oslo).size)
    }
    @Test fun multipleDaysPreserveTotal() {
        val start = time("2026-10-24T23:57:42+02:00[Europe/Oslo]")
        val duration = 5 * 86_400_000L + 777
        assertEquals(duration, UsageMath.split(start, start + duration, oslo).values.sum())
    }
    @Test fun timerIncludesSecondsAndLongHours() {
        assertEquals("00:00:00", UsageMath.clock(-10)); assertEquals("00:18:42", UsageMath.clock(1_122_999)); assertEquals("100:00:00", UsageMath.clock(360_000_000))
    }
}
