package com.weathermorning.alarm.domain.usecase

import com.weathermorning.alarm.domain.model.WeatherCheckWindow
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCheckTargetsTest {
    @Test
    fun alarmTimeWindowChecksOnlyAlarmHour() {
        val alarmAt = LocalDateTime.of(2026, 6, 17, 7, 30)

        assertEquals(
            listOf(alarmAt),
            WeatherCheckTargets.forWindow(WeatherCheckWindow.ALARM_TIME, alarmAt),
        )
    }

    @Test
    fun commuteWindowCrossesMidnightSafely() {
        val alarmAt = LocalDateTime.of(2026, 6, 18, 0, 30)

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 6, 17, 23, 30),
                LocalDateTime.of(2026, 6, 18, 0, 30),
                LocalDateTime.of(2026, 6, 18, 1, 30),
                LocalDateTime.of(2026, 6, 18, 2, 30),
            ),
            WeatherCheckTargets.forWindow(WeatherCheckWindow.COMMUTE_WINDOW, alarmAt),
        )
    }
}
