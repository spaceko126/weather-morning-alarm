package com.weathermorning.alarm.data.repository

import android.content.Context
import androidx.core.content.edit
import com.weathermorning.alarm.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Korean public holidays, fetched once a year from the backend proxy and cached
 * on disk.
 *
 * [com.weathermorning.alarm.alarm.KoreanPublicHolidayCalendar] only knows
 * fixed-date holidays; this fills in Lunar New Year, Chuseok and substitute
 * holidays. When the backend has no holiday source configured the cache stays
 * empty and the fixed-date calendar keeps working on its own.
 */
class HolidayRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("holiday_cache", Context.MODE_PRIVATE)

    /** Cached answer only. Safe to call from a broadcast's main thread. */
    fun cachedHolidays(year: Int): Set<LocalDate> {
        val stored = prefs.getString(key(year), null) ?: return emptySet()
        return stored.split(",")
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
    }

    fun hasCache(year: Int): Boolean = prefs.contains(key(year))

    /** Fetches and caches [year] unless it is already cached. */
    suspend fun refresh(year: Int): Result<Set<LocalDate>> = withContext(Dispatchers.IO) {
        if (hasCache(year)) return@withContext Result.success(cachedHolidays(year))
        val baseUrl = BuildConfig.WEATHER_PROXY_BASE_URL.trim()
        if (baseUrl.isBlank()) return@withContext Result.success(emptySet())

        runCatching {
            val body = request("${baseUrl.trimEnd('/')}/holidays?year=$year&country=KR")
            val dates = parse(body)
            prefs.edit { putString(key(year), dates.joinToString(",") { it.toString() }) }
            dates
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
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Holiday request failed: HTTP $code")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): Set<LocalDate> {
        val array = JSONObject(body).optJSONArray("holidays") ?: return emptySet()
        return buildSet {
            for (index in 0 until array.length()) {
                val date = array.getJSONObject(index).optString("date")
                runCatching { LocalDate.parse(date) }.getOrNull()?.let(::add)
            }
        }
    }

    private fun key(year: Int) = "holidays_$year"
}
