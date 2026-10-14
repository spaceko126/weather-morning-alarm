package com.weathermorning.alarm.data.local

import android.content.Context
import androidx.core.content.edit
import com.weathermorning.alarm.domain.model.AlarmDecision
import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * One saved decision per alarm. Each alarm gets its own prefs file so deleting
 * an alarm cannot strand another alarm's decision.
 */
class LocalAlarmDecisionStore(context: Context, alarmId: Long = 1L) {
    private val prefs = context.getSharedPreferences("alarm_decision_$alarmId", Context.MODE_PRIVATE)

    fun save(decision: AlarmDecision) {
        val snapshots = decision.weatherSnapshots.ifEmpty {
            decision.weatherSnapshot?.let(::listOf).orEmpty()
        }
        prefs.edit {
            putBoolean("should_ring", decision.shouldRing)
            putString("reason", decision.reason)
            putString("reason_code", decision.reasonCode?.name)
            putString("reason_condition", decision.reasonCondition?.name)
            putInt("reason_value_1", decision.reasonValue1 ?: -1)
            putInt("reason_value_2", decision.reasonValue2 ?: -1)
            putString("decided_at", decision.decidedAt.toString())
            putString("condition", decision.weatherSnapshot?.condition?.name)
            putInt("precipitation_probability", decision.weatherSnapshot?.precipitationProbability ?: -1)
            putString("precipitation_amount_mm", decision.weatherSnapshot?.precipitationAmountMm?.toString())
            putString("checked_at", decision.weatherSnapshot?.checkedAt?.toString())
            putString("target_date_time", decision.weatherSnapshot?.targetDateTime?.toString())
            putString("source", decision.weatherSnapshot?.source)
            putString("weather_snapshots", snapshots.toJsonArray().toString())
        }
    }

    fun load(): AlarmDecision? {
        if (!prefs.contains("should_ring")) return null

        val weather = runCatching {
            val conditionName = prefs.getString("condition", null) ?: return@runCatching null
            val checkedAt = prefs.getString("checked_at", null) ?: return@runCatching null
            val targetDateTime = prefs.getString("target_date_time", null) ?: return@runCatching null
            WeatherSnapshot(
                checkedAt = Instant.parse(checkedAt),
                targetDateTime = LocalDateTime.parse(targetDateTime),
                condition = WeatherCondition.valueOf(conditionName),
                precipitationProbability = prefs.getInt("precipitation_probability", 0),
                precipitationAmountMm = prefs.getString("precipitation_amount_mm", "0.0")
                    ?.toDoubleOrNull() ?: 0.0,
                source = prefs.getString("source", "local") ?: "local",
            )
        }.getOrNull()

        val weatherSnapshots = prefs.getString("weather_snapshots", null)
            ?.toWeatherSnapshots()
            .orEmpty()
            .ifEmpty { weather?.let(::listOf).orEmpty() }

        return AlarmDecision(
            shouldRing = prefs.getBoolean("should_ring", false),
            reason = prefs.getString("reason", "No reason saved") ?: "No reason saved",
            decidedAt = runCatching {
                Instant.parse(prefs.getString("decided_at", null) ?: Instant.now().toString())
            }.getOrDefault(Instant.now()),
            weatherSnapshot = weather,
            weatherSnapshots = weatherSnapshots,
            reasonCode = prefs.getString("reason_code", null)?.let { code ->
                runCatching { AlarmReasonCode.valueOf(code) }.getOrNull()
            },
            reasonCondition = prefs.getString("reason_condition", null)?.let { condition ->
                runCatching { WeatherCondition.valueOf(condition) }.getOrNull()
            },
            reasonValue1 = prefs.getIntOrNull("reason_value_1"),
            reasonValue2 = prefs.getIntOrNull("reason_value_2"),
        )
    }

    fun clear() {
        prefs.edit { clear() }
    }
}

private fun android.content.SharedPreferences.getIntOrNull(key: String): Int? {
    if (!contains(key)) return null
    return getInt(key, -1).takeIf { it >= 0 }
}

private fun List<WeatherSnapshot>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { weather ->
            array.put(
                JSONObject()
                    .put("checkedAt", weather.checkedAt.toString())
                    .put("targetDateTime", weather.targetDateTime.toString())
                    .put("condition", weather.condition.name)
                    .put("precipitationProbability", weather.precipitationProbability)
                    .put("precipitationAmountMm", weather.precipitationAmountMm)
                    .put("source", weather.source),
            )
        }
    }
}

private fun String.toWeatherSnapshots(): List<WeatherSnapshot> {
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    WeatherSnapshot(
                        checkedAt = Instant.parse(item.getString("checkedAt")),
                        targetDateTime = LocalDateTime.parse(item.getString("targetDateTime")),
                        condition = WeatherCondition.valueOf(item.getString("condition")),
                        precipitationProbability = item.getInt("precipitationProbability"),
                        precipitationAmountMm = item.optDouble("precipitationAmountMm", 0.0),
                        source = item.optString("source", "local"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}
