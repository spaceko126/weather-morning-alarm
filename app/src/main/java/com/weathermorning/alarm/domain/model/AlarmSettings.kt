package com.weathermorning.alarm.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class AlarmSettings(
    val id: Long = 1L,
    val label: String = "",
    val enabled: Boolean,
    val alarmTime: LocalTime,
    val repeatDays: Set<DayOfWeek>,
    val excludePublicHolidays: Boolean,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val triggerRain: Boolean,
    val triggerSnow: Boolean,
    val precipitationThreshold: Int,
    val rainAmountThreshold: RainAmountThreshold = RainAmountThreshold.NONE,
    val weatherCheckWindow: WeatherCheckWindow = WeatherCheckWindow.ALARM_TIME,
    val failPolicy: FailPolicy,
    val alarmSoundEnabled: Boolean = true,
    val alarmSoundName: AlarmSoundName = AlarmSoundName.DEFAULT,
    val alarmSoundUri: String = "",
    val alarmSoundTitle: String = "",
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val snoozeMaxCount: Int = 3,
) {
    companion object {
        val Default = AlarmSettings(
            id = 1L,
            label = "",
            enabled = false,
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            excludePublicHolidays = false,
            locationName = "Seoul",
            latitude = 37.5665,
            longitude = 126.9780,
            triggerRain = true,
            triggerSnow = true,
            precipitationThreshold = 40,
            rainAmountThreshold = RainAmountThreshold.NONE,
            weatherCheckWindow = WeatherCheckWindow.ALARM_TIME,
            failPolicy = FailPolicy.RING,
            alarmSoundEnabled = true,
            alarmSoundName = AlarmSoundName.DEFAULT,
            alarmSoundUri = "",
            alarmSoundTitle = "",
            vibrationEnabled = true,
            snoozeEnabled = true,
            snoozeMinutes = 5,
            snoozeMaxCount = 3,
        )
    }
}

enum class FailPolicy {
    RING,
    SKIP,
}

enum class WeatherCheckWindow {
    ALARM_TIME,
    COMMUTE_WINDOW,
}

enum class RainAmountThreshold(
    val minMmPerHour: Double,
) {
    NONE(0.0),
    DRIZZLE(0.1),
    LIGHT(2.0),
    HEAVY(10.0),
}

val SNOOZE_MINUTE_OPTIONS = listOf(1, 5, 10, 15, 30)

enum class AlarmSoundName {
    DEFAULT,
    SOFT_RAIN,
    BRIGHT_BELL,
}
