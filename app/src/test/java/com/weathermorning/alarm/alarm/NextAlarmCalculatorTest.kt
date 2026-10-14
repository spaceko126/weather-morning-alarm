package com.weathermorning.alarm.alarm

import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.FailPolicy
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextAlarmCalculatorTest {
    @Test
    fun disabledAlarmReturnsNull() {
        val settings = settings(enabled = false)
        val now = LocalDateTime.of(2026, 6, 17, 6, 0)

        assertNull(NextAlarmCalculator.nextAlarmDateTime(settings, now))
    }

    @Test
    fun emptyRepeatDaysReturnsNull() {
        val settings = settings(repeatDays = emptySet())
        val now = LocalDateTime.of(2026, 6, 17, 6, 0)

        assertNull(NextAlarmCalculator.nextAlarmDateTime(settings, now))
    }

    @Test
    fun selectedTodayFutureTimeReturnsToday() {
        val now = LocalDateTime.of(2026, 6, 17, 6, 0)
        val settings = settings(
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.WEDNESDAY),
        )

        assertEquals(
            LocalDateTime.of(2026, 6, 17, 7, 0),
            NextAlarmCalculator.nextAlarmDateTime(settings, now),
        )
    }

    @Test
    fun selectedTodayPastTimeReturnsNextWeek() {
        val now = LocalDateTime.of(2026, 6, 17, 8, 0)
        val settings = settings(
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.WEDNESDAY),
        )

        assertEquals(
            LocalDateTime.of(2026, 6, 24, 7, 0),
            NextAlarmCalculator.nextAlarmDateTime(settings, now),
        )
    }

    @Test
    fun choosesNearestSelectedFutureDay() {
        val now = LocalDateTime.of(2026, 6, 17, 8, 0)
        val settings = settings(
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
        )

        assertEquals(
            LocalDateTime.of(2026, 6, 19, 7, 0),
            NextAlarmCalculator.nextAlarmDateTime(settings, now),
        )
    }

    @Test
    fun publicHolidayExcludedWeekdaySkipsFixedHoliday() {
        val now = LocalDateTime.of(2026, 12, 30, 8, 0)
        val settings = settings(
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
            excludePublicHolidays = true,
        )

        assertEquals(
            LocalDateTime.of(2027, 1, 4, 7, 0),
            NextAlarmCalculator.nextAlarmDateTime(settings, now),
        )
    }

    @Test
    fun publicHolidayAllowedWeekdayCanUseFixedHoliday() {
        val now = LocalDateTime.of(2026, 12, 30, 8, 0)
        val settings = settings(
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
            excludePublicHolidays = false,
        )

        assertEquals(
            LocalDateTime.of(2027, 1, 1, 7, 0),
            NextAlarmCalculator.nextAlarmDateTime(settings, now),
        )
    }

    private fun settings(
        enabled: Boolean = true,
        alarmTime: LocalTime = LocalTime.of(7, 0),
        repeatDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
        excludePublicHolidays: Boolean = false,
    ): AlarmSettings {
        return AlarmSettings(
            enabled = enabled,
            alarmTime = alarmTime,
            repeatDays = repeatDays,
            excludePublicHolidays = excludePublicHolidays,
            locationName = "Seoul",
            latitude = 37.5665,
            longitude = 126.9780,
            triggerRain = true,
            triggerSnow = true,
            precipitationThreshold = 40,
            failPolicy = FailPolicy.RING,
        )
    }
}
