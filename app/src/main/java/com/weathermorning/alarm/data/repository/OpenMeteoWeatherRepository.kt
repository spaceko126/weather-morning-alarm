package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.domain.repository.WeatherRepository
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenMeteoWeatherRepository : WeatherRepository {
    override suspend fun getForecast(
        settings: AlarmSettings,
        targetDateTime: java.time.LocalDateTime,
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val body = request(buildUrl(settings))
            OpenMeteoForecastParser.parse(body, targetDateTime)
        }
    }

    private fun buildUrl(settings: AlarmSettings): String {
        return buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=").append(settings.latitude.formatCoordinate())
            append("&longitude=").append(settings.longitude.formatCoordinate())
            append("&hourly=precipitation_probability,precipitation,weather_code")
            append("&timezone=auto")
            append("&forecast_days=3")
        }
    }

    private fun request(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) error("Open-Meteo request failed: HTTP $code $body")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun Double.formatCoordinate(): String {
        return String.format(Locale.US, "%.6f", this)
    }
}
