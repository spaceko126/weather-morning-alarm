package com.weathermorning.alarm.alarm

import com.weathermorning.alarm.domain.model.AlarmSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmNotificationChannelsTest {
    @Test
    fun channelIdIsSilentFullScreenChannel() {
        val soundVibrate = AlarmNotificationChannels.channelId(
            AlarmSettings.Default.copy(
                alarmSoundEnabled = true,
                vibrationEnabled = true,
                alarmSoundUri = "content://alarm/one",
            ),
        )
        val silentStill = AlarmNotificationChannels.channelId(
            AlarmSettings.Default.copy(
                alarmSoundEnabled = false,
                vibrationEnabled = false,
                alarmSoundUri = "content://alarm/one",
            ),
        )

        assertEquals("weather_morning_alarm_screen_silent", soundVibrate)
        assertEquals("weather_morning_alarm_screen_silent", silentStill)
    }

    @Test
    fun staleChannelIdsOnlyReturnsOldWeatherAlarmChannels() {
        val current = AlarmNotificationChannels.channelId(
            AlarmSettings.Default.copy(alarmSoundUri = "content://alarm/current"),
        )
        val oldWeather = AlarmNotificationChannels.channelId(
            AlarmSettings.Default.copy(alarmSoundUri = "content://alarm/old"),
        )
        val legacySoundChannel = "weather_morning_alarm_sound_vibrate_123"
        val unrelated = "marketing_updates"
        val legacyBase = "weather_morning_alarm"

        val stale = AlarmNotificationChannels.staleChannelIds(
            allChannelIds = listOf(current, oldWeather, legacySoundChannel, unrelated, legacyBase),
            currentChannelId = current,
        )

        assertEquals(listOf(legacySoundChannel), stale)
        assertFalse(stale.contains(current))
        assertFalse(stale.contains(oldWeather))
        assertFalse(stale.contains(unrelated))
        assertFalse(stale.contains(legacyBase))
    }
}
