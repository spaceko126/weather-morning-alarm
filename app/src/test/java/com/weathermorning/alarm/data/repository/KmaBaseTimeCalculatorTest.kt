package com.weathermorning.alarm.data.repository

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class KmaBaseTimeCalculatorTest {
    @Test
    fun usesLatestBaseTimeOlderThanTwentyMinutes() {
        val base = KmaBaseTimeCalculator.forTarget(
            targetDateTime = LocalDateTime.of(2026, 6, 17, 7, 0),
            now = LocalDateTime.of(2026, 6, 17, 10, 30),
        )

        assertEquals(LocalDate.of(2026, 6, 17), base.date)
        assertEquals("0800", base.time)
    }

    @Test
    fun beforeFirstBaseTimeUsesPreviousDay2300() {
        val base = KmaBaseTimeCalculator.forTarget(
            targetDateTime = LocalDateTime.of(2026, 6, 17, 7, 0),
            now = LocalDateTime.of(2026, 6, 17, 2, 10),
        )

        assertEquals(LocalDate.of(2026, 6, 16), base.date)
        assertEquals("2300", base.time)
    }

    @Test
    fun afterLateBaseTimeUsesSameDay2300() {
        val base = KmaBaseTimeCalculator.forTarget(
            targetDateTime = LocalDateTime.of(2026, 6, 18, 7, 0),
            now = LocalDateTime.of(2026, 6, 17, 23, 50),
        )

        assertEquals(LocalDate.of(2026, 6, 17), base.date)
        assertEquals("2300", base.time)
    }

    @Test
    fun justBefore0500AvailabilityUses0200() {
        val base = KmaBaseTimeCalculator.forTarget(
            targetDateTime = LocalDateTime.of(2026, 6, 17, 7, 0),
            now = LocalDateTime.of(2026, 6, 17, 5, 10),
        )

        assertEquals(LocalDate.of(2026, 6, 17), base.date)
        assertEquals("0200", base.time)
    }
}
