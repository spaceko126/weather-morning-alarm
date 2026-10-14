package com.weathermorning.alarm.domain.usecase

import com.weathermorning.alarm.domain.model.WeatherCheckWindow
import java.time.LocalDateTime

object WeatherCheckTargets {
    fun forWindow(
        window: WeatherCheckWindow,
        alarmDateTime: LocalDateTime,
    ): List<LocalDateTime> {
        return when (window) {
            WeatherCheckWindow.ALARM_TIME -> listOf(alarmDateTime)
            WeatherCheckWindow.COMMUTE_WINDOW -> (-1L..2L).map { alarmDateTime.plusHours(it) }
        }
    }
}
