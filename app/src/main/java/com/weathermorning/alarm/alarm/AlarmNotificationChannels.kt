package com.weathermorning.alarm.alarm

import com.weathermorning.alarm.domain.model.AlarmSettings

object AlarmNotificationChannels {
    private const val BASE_ID = "weather_morning_alarm_screen"
    private const val PREFIX = "${BASE_ID}_"
    private const val LEGACY_PREFIX = "weather_morning_alarm_"

    fun channelId(@Suppress("UNUSED_PARAMETER") settings: AlarmSettings): String {
        return "${BASE_ID}_silent"
    }

    fun staleChannelIds(
        allChannelIds: Iterable<String>,
        currentChannelId: String,
    ): List<String> {
        return allChannelIds
            .filter {
                it != currentChannelId &&
                    (it.startsWith(PREFIX) || it.startsWith(LEGACY_PREFIX))
            }
    }
}
