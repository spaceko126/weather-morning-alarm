package com.weathermorning.alarm.data.local

import android.content.Context
import androidx.core.content.edit
import com.weathermorning.alarm.R
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.AlarmSoundName
import com.weathermorning.alarm.domain.model.FailPolicy
import com.weathermorning.alarm.domain.model.RainAmountThreshold
import com.weathermorning.alarm.domain.model.WeatherCheckWindow
import java.time.DayOfWeek
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Alarms live as a JSON array in SharedPreferences rather than in Room.
 *
 * [com.weathermorning.alarm.alarm.AlarmReceiver] reads them on a broadcast's
 * main thread, where a database open would be too slow; the list is a handful
 * of small records, so prefs are the right size of tool.
 */
class LocalAlarmSettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE)

    fun loadAll(): List<AlarmSettings> {
        val stored = prefs.getString(KEY_ALARMS, null)
            ?: return listOf(migrateLegacySingleAlarm()).also(::saveAll)
        return runCatching { stored.toAlarms() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(AlarmSettings.Default)
    }

    fun load(id: Long): AlarmSettings? = loadAll().firstOrNull { it.id == id }

    /** Inserts or replaces one alarm, keeping the list ordered by time. */
    fun save(alarm: AlarmSettings) {
        val current = loadAll().filterNot { it.id == alarm.id }
        saveAll(current + alarm)
    }

    fun delete(id: Long) {
        val remaining = loadAll().filterNot { it.id == id }
        saveAll(remaining)
    }

    fun nextId(): Long = (loadAll().maxOfOrNull { it.id } ?: 0L) + 1L

    private fun saveAll(alarms: List<AlarmSettings>) {
        val ordered = alarms.sortedWith(compareBy({ it.alarmTime }, { it.id }))
        prefs.edit { putString(KEY_ALARMS, ordered.toJsonArray().toString()) }
    }

    /**
     * Builds the single pre-WM-128 alarm from the flat keys it used to live
     * under, so an existing install keeps its alarm on upgrade.
     */
    private fun migrateLegacySingleAlarm(): AlarmSettings {
        val default = AlarmSettings.Default
        if (!prefs.contains("hour")) return default

        val repeatDays = prefs.getString("repeat_days", null)
            ?.split(",")
            ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: default.repeatDays

        return default.copy(
            id = 1L,
            enabled = prefs.getBoolean("enabled", default.enabled),
            alarmTime = LocalTime.of(
                prefs.getInt("hour", default.alarmTime.hour),
                prefs.getInt("minute", default.alarmTime.minute),
            ),
            repeatDays = repeatDays,
            excludePublicHolidays = prefs.getBoolean("exclude_public_holidays", default.excludePublicHolidays),
            locationName = sanitizeLocationName(
                prefs.getString("location_name", default.locationName) ?: default.locationName,
            ),
            latitude = prefs.getString("latitude", null)?.toDoubleOrNull() ?: default.latitude,
            longitude = prefs.getString("longitude", null)?.toDoubleOrNull() ?: default.longitude,
            triggerRain = prefs.getBoolean("trigger_rain", default.triggerRain),
            triggerSnow = prefs.getBoolean("trigger_snow", default.triggerSnow),
            precipitationThreshold = prefs.getInt("precipitation_threshold", default.precipitationThreshold),
            rainAmountThreshold = prefs.enumOr("rain_amount_threshold", default.rainAmountThreshold),
            weatherCheckWindow = prefs.enumOr("weather_check_window", default.weatherCheckWindow),
            failPolicy = prefs.enumOr("fail_policy", default.failPolicy),
            alarmSoundEnabled = prefs.getBoolean("alarm_sound_enabled", default.alarmSoundEnabled),
            alarmSoundName = prefs.enumOr("alarm_sound_name", default.alarmSoundName),
            alarmSoundUri = prefs.getString("alarm_sound_uri", default.alarmSoundUri) ?: default.alarmSoundUri,
            alarmSoundTitle = prefs.getString("alarm_sound_title", default.alarmSoundTitle) ?: default.alarmSoundTitle,
            vibrationEnabled = prefs.getBoolean("vibration_enabled", default.vibrationEnabled),
            snoozeEnabled = prefs.getBoolean("snooze_enabled", default.snoozeEnabled),
            snoozeMinutes = prefs.getInt("snooze_minutes", default.snoozeMinutes).coerceIn(1, 60),
            snoozeMaxCount = prefs.getInt("snooze_max_count", default.snoozeMaxCount).coerceIn(1, 10),
        )
    }

    private fun sanitizeLocationName(name: String): String {
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        val currentLocation = context.getString(R.string.current_location_label)
        // "현재 위치" is matched too, so names saved by older builds keep working.
        val legacyKoreanLabel = "현재 위치"

        return when {
            lower.startsWith("current location") -> currentLocation
            trimmed.startsWith(legacyKoreanLabel) -> currentLocation
            trimmed.startsWith(currentLocation) -> currentLocation
            lower.contains("(network)") ||
                lower.contains("(gps)") ||
                lower.contains("(passive)") -> {
                trimmed.substringBefore("(").trim().ifBlank { currentLocation }
            }
            else -> trimmed.ifBlank { currentLocation }
        }
    }

    private companion object {
        const val KEY_ALARMS = "alarms_json"
    }
}

private inline fun <reified T : Enum<T>> android.content.SharedPreferences.enumOr(
    key: String,
    fallback: T,
): T {
    val name = getString(key, null) ?: return fallback
    return runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
}

private fun List<AlarmSettings>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { alarm ->
            array.put(
                JSONObject()
                    .put("id", alarm.id)
                    .put("label", alarm.label)
                    .put("enabled", alarm.enabled)
                    .put("hour", alarm.alarmTime.hour)
                    .put("minute", alarm.alarmTime.minute)
                    .put("repeatDays", alarm.repeatDays.joinToString(",") { it.name })
                    .put("excludePublicHolidays", alarm.excludePublicHolidays)
                    .put("locationName", alarm.locationName)
                    .put("latitude", alarm.latitude)
                    .put("longitude", alarm.longitude)
                    .put("triggerRain", alarm.triggerRain)
                    .put("triggerSnow", alarm.triggerSnow)
                    .put("precipitationThreshold", alarm.precipitationThreshold)
                    .put("rainAmountThreshold", alarm.rainAmountThreshold.name)
                    .put("weatherCheckWindow", alarm.weatherCheckWindow.name)
                    .put("failPolicy", alarm.failPolicy.name)
                    .put("alarmSoundEnabled", alarm.alarmSoundEnabled)
                    .put("alarmSoundName", alarm.alarmSoundName.name)
                    .put("alarmSoundUri", alarm.alarmSoundUri)
                    .put("alarmSoundTitle", alarm.alarmSoundTitle)
                    .put("vibrationEnabled", alarm.vibrationEnabled)
                    .put("snoozeEnabled", alarm.snoozeEnabled)
                    .put("snoozeMinutes", alarm.snoozeMinutes)
                    .put("snoozeMaxCount", alarm.snoozeMaxCount),
            )
        }
    }
}

private fun String.toAlarms(): List<AlarmSettings> {
    val default = AlarmSettings.Default
    val array = JSONArray(this)
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                AlarmSettings(
                    id = item.optLong("id", (index + 1).toLong()),
                    label = item.optString("label", ""),
                    enabled = item.optBoolean("enabled", false),
                    alarmTime = LocalTime.of(
                        item.optInt("hour", default.alarmTime.hour).coerceIn(0, 23),
                        item.optInt("minute", default.alarmTime.minute).coerceIn(0, 59),
                    ),
                    repeatDays = item.optString("repeatDays")
                        .split(",")
                        .mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                        .toSet(),
                    excludePublicHolidays = item.optBoolean("excludePublicHolidays", false),
                    locationName = item.optString("locationName", default.locationName),
                    latitude = item.optDouble("latitude", default.latitude),
                    longitude = item.optDouble("longitude", default.longitude),
                    triggerRain = item.optBoolean("triggerRain", default.triggerRain),
                    triggerSnow = item.optBoolean("triggerSnow", default.triggerSnow),
                    precipitationThreshold = item.optInt("precipitationThreshold", default.precipitationThreshold),
                    rainAmountThreshold = item.enumOr("rainAmountThreshold", default.rainAmountThreshold),
                    weatherCheckWindow = item.enumOr("weatherCheckWindow", default.weatherCheckWindow),
                    failPolicy = item.enumOr("failPolicy", default.failPolicy),
                    alarmSoundEnabled = item.optBoolean("alarmSoundEnabled", default.alarmSoundEnabled),
                    alarmSoundName = item.enumOr("alarmSoundName", default.alarmSoundName),
                    alarmSoundUri = item.optString("alarmSoundUri", ""),
                    alarmSoundTitle = item.optString("alarmSoundTitle", ""),
                    vibrationEnabled = item.optBoolean("vibrationEnabled", default.vibrationEnabled),
                    snoozeEnabled = item.optBoolean("snoozeEnabled", default.snoozeEnabled),
                    snoozeMinutes = item.optInt("snoozeMinutes", default.snoozeMinutes).coerceIn(1, 60),
                    snoozeMaxCount = item.optInt("snoozeMaxCount", default.snoozeMaxCount).coerceIn(1, 10),
                ),
            )
        }
    }
}

private inline fun <reified T : Enum<T>> JSONObject.enumOr(key: String, fallback: T): T {
    val name = optString(key).takeIf { it.isNotBlank() } ?: return fallback
    return runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
}
