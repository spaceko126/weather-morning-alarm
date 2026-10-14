package com.weathermorning.alarm.data.repository

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One place the user can search for, as returned by the geocoding service. */
data class GeocodedPlace(
    val name: String,
    val admin: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** "Seocho-gu, Seoul, South Korea" with the blanks dropped. */
    val displayName: String
        get() = listOf(name, admin, country)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
}

/**
 * Replaces the nine hardcoded presets. Uses Open-Meteo's geocoding service,
 * which needs no key and answers in the device language.
 */
class GeocodingSearchRepository(
    private val baseUrl: String = "https://geocoding-api.open-meteo.com/v1/search",
) {
    suspend fun search(query: String, limit: Int = 12): Result<List<GeocodedPlace>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.length < MIN_QUERY_LENGTH) return@withContext Result.success(emptyList())
            runCatching { parse(request(buildUrl(trimmed, limit))) }
        }

    private fun buildUrl(query: String, limit: Int): String {
        return buildString {
            append(baseUrl)
            append("?name=").append(query.urlEncode())
            append("&count=").append(limit)
            append("&language=").append(Locale.getDefault().language.ifBlank { "en" })
            append("&format=json")
        }
    }

    private fun request(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Geocoding request failed: HTTP $code")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): List<GeocodedPlace> {
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val item = results.getJSONObject(index)
                val name = item.optString("name")
                if (name.isBlank()) continue
                add(
                    GeocodedPlace(
                        name = name,
                        admin = item.optString("admin1"),
                        country = item.optString("country"),
                        latitude = item.optDouble("latitude", Double.NaN),
                        longitude = item.optDouble("longitude", Double.NaN),
                    ),
                )
            }
        }.filterNot { it.latitude.isNaN() || it.longitude.isNaN() }
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
