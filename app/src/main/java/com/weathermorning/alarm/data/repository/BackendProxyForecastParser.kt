package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDateTime
import org.json.JSONObject

object BackendProxyForecastParser {
    fun parse(body: String): WeatherSnapshot {
        val json = JSONObject(body)
        val providerSource = json.optString("source", "forecast")
        return WeatherSnapshot(
            checkedAt = Instant.parse(json.getString("checkedAt")),
            targetDateTime = LocalDateTime.parse(json.getString("targetDateTime")),
            condition = WeatherCondition.valueOf(json.getString("condition")),
            precipitationProbability = json.getInt("precipitationProbability"),
            precipitationAmountMm = json.optDouble("precipitationAmountMm", 0.0),
            source = "backend-proxy:$providerSource",
        )
    }
}
