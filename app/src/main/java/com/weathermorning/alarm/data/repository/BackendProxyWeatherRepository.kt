package com.weathermorning.alarm.data.repository

import android.content.Context
import com.weathermorning.alarm.BuildConfig
import com.weathermorning.alarm.data.local.BackendProxyClientIdStore
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.domain.repository.WeatherRepository
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BackendProxyWeatherRepository(
    private val baseUrl: String,
    private val clientIdProvider: () -> String? = { null },
) : WeatherRepository {
    override suspend fun getForecast(
        settings: AlarmSettings,
        targetDateTime: LocalDateTime,
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val body = request(buildUrl(settings, targetDateTime))
            BackendProxyForecastParser.parse(body)
        }
    }

    private fun buildUrl(settings: AlarmSettings, targetDateTime: LocalDateTime): String {
        return buildString {
            append(baseUrl.trimEnd('/'))
            append("/weather/forecast")
            append("?latitude=").append(settings.latitude.formatCoordinate().urlEncode())
            append("&longitude=").append(settings.longitude.formatCoordinate().urlEncode())
            append("&target=").append(targetDateTime.toString().urlEncode())
            append("&locale=").append(Locale.getDefault().toLanguageTag().urlEncode())
        }
    }

    private fun request(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        val clientId = clientIdProvider()
        if (!clientId.isNullOrBlank()) {
            connection.setRequestProperty("x-weather-client-id", clientId)
        }

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error(proxyErrorMessage(code, body))
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun proxyErrorMessage(code: Int, body: String): String {
        val message = runCatching {
            val json = JSONObject(body)
            json.optString("message").ifBlank { json.optString("error") }
        }.getOrDefault("")
        return if (message.isBlank()) {
            "Backend proxy request failed: HTTP $code"
        } else {
            "Backend proxy request failed: HTTP $code $message"
        }
    }

    private fun Double.formatCoordinate(): String {
        return String.format(Locale.US, "%.6f", this)
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
    }

    companion object {
        fun fromBuildConfig(context: Context? = null): WeatherRepository? {
            val configuredBaseUrl = BuildConfig.WEATHER_PROXY_BASE_URL.trim()
            val clientIdProvider: () -> String? = if (context != null) {
                val store = BackendProxyClientIdStore(context.applicationContext)
                ({ store.getOrCreate() })
            } else {
                ({ null })
            }
            return configuredBaseUrl.takeIf { it.isNotBlank() }?.let {
                BackendProxyWeatherRepository(it, clientIdProvider)
            }
        }
    }
}
