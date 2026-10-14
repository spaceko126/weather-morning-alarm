package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

object OpenMeteoForecastParser {
    fun parse(body: String, targetDateTime: LocalDateTime): WeatherSnapshot {
        val root = JSONObject(body)
        if (root.optBoolean("error", false)) {
            error("Open-Meteo error: ${root.optString("reason", "unknown")}")
        }

        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val precipitationProbability = hourly.getJSONArray("precipitation_probability")
        val precipitationAmount = hourly.optJSONArray("precipitation")
        val weatherCodes = hourly.getJSONArray("weather_code")
        val targetHour = targetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))

        for (index in 0 until times.length()) {
            if (times.getString(index) != targetHour) continue

            return WeatherSnapshot(
                checkedAt = Instant.now(),
                targetDateTime = targetDateTime,
                condition = conditionFromWeatherCode(weatherCodes.getInt(index)),
                precipitationProbability = precipitationProbability.optInt(index, 0),
                precipitationAmountMm = precipitationAmount?.optDouble(index, 0.0) ?: 0.0,
                source = "open-meteo:forecast",
            )
        }

        error("Open-Meteo forecast hour not found: $targetHour")
    }

    private fun conditionFromWeatherCode(code: Int): WeatherCondition {
        return when (code) {
            56, 57, 66, 67 -> WeatherCondition.SLEET
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
            51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99 -> WeatherCondition.RAIN
            else -> WeatherCondition.CLEAR
        }
    }
}
