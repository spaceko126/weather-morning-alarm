package com.weathermorning.alarm.debug

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.weathermorning.alarm.alarm.AlarmReceiver
import com.weathermorning.alarm.alarm.AlarmService
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.presentation.AlarmActivity
import java.time.Instant
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject

class DebugAlarmTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_CANCEL_ONLY, false) == true) {
            context.getSystemService(NotificationManager::class.java).cancel(ALARM_NOTIFICATION_ID)
            NotificationManagerCompat.from(context).cancel(ALARM_NOTIFICATION_ID)
            context.getSystemService(NotificationManager::class.java).cancelAll()
            return
        }

        // Drives AlarmService directly so stop/snooze can be checked from adb;
        // the service itself stays unexported.
        val serviceAction = intent?.getStringExtra(EXTRA_SERVICE_ACTION)
        if (serviceAction != null) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java).setAction(
                    when (serviceAction) {
                        "snooze" -> AlarmService.ACTION_SNOOZE
                        else -> AlarmService.ACTION_STOP
                    },
                ),
            )
            return
        }

        val settingsStore = LocalAlarmSettingsStore(context)
        val current = settingsStore.loadAll().firstOrNull() ?: return
        val testSettings = current.copy(
            alarmSoundEnabled = intent?.getBooleanExtra(EXTRA_SOUND, current.alarmSoundEnabled)
                ?: current.alarmSoundEnabled,
            vibrationEnabled = intent?.getBooleanExtra(EXTRA_VIBRATION, current.vibrationEnabled)
                ?: current.vibrationEnabled,
        )

        settingsStore.save(testSettings)
        if (intent?.getBooleanExtra(EXTRA_FORECAST_SCREEN, false) == true) {
            context.startActivity(
                Intent(context, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(AlarmActivity.EXTRA_REASON, "Debug forecast alarm")
                    .putExtra(AlarmActivity.EXTRA_WEATHER_CONDITION, WeatherCondition.RAIN.name)
                    .putExtra(AlarmActivity.EXTRA_PRECIPITATION_PROBABILITY, 70)
                    .putExtra(AlarmActivity.EXTRA_PRECIPITATION_AMOUNT_MM, 2.5)
                    .putExtra(AlarmActivity.EXTRA_WEATHER_SOURCE, "debug")
                    .putExtra(AlarmActivity.EXTRA_TARGET_TIME, "07:00")
                    .putExtra(AlarmActivity.EXTRA_FORECAST_SNAPSHOTS, debugForecastRows().toJsonArray().toString()),
            )
            return
        }

        AlarmReceiver().onReceive(
            context,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.EXTRA_TEST_ALARM, true),
        )
    }

    companion object {
        const val EXTRA_SOUND = "sound"
        const val EXTRA_VIBRATION = "vibration"
        const val EXTRA_CANCEL_ONLY = "cancelOnly"
        const val EXTRA_FORECAST_SCREEN = "forecastScreen"
        const val EXTRA_SERVICE_ACTION = "serviceAction"
        private const val ALARM_NOTIFICATION_ID = 7001
    }
}

private fun debugForecastRows(): List<WeatherSnapshot> {
    val checkedAt = Instant.parse("2026-06-25T21:00:00Z")
    val date = LocalDateTime.of(2026, 6, 26, 7, 0)
    return listOf(
        WeatherSnapshot(checkedAt, date.minusHours(1), WeatherCondition.CLEAR, 20, 0.0, "debug"),
        WeatherSnapshot(checkedAt, date, WeatherCondition.RAIN, 70, 2.5, "debug"),
        WeatherSnapshot(checkedAt, date.plusHours(1), WeatherCondition.RAIN, 80, 4.0, "debug"),
        WeatherSnapshot(checkedAt, date.plusHours(2), WeatherCondition.SLEET, 60, 1.2, "debug"),
    )
}

private fun List<WeatherSnapshot>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { weather ->
            array.put(
                JSONObject()
                    .put("targetTime", weather.targetDateTime.toLocalTime().toString())
                    .put("condition", weather.condition.name)
                    .put("precipitationProbability", weather.precipitationProbability)
                    .put("precipitationAmountMm", weather.precipitationAmountMm),
            )
        }
    }
}
