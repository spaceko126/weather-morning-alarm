package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.domain.repository.WeatherRepository
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KmaWeatherRepository(
    private val serviceKey: String,
) : WeatherRepository {
    override suspend fun getForecast(
        settings: AlarmSettings,
        targetDateTime: LocalDateTime,
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val grid = KmaGridConverter.toGrid(settings.latitude, settings.longitude)
            val base = KmaBaseTimeCalculator.forTarget(targetDateTime)
            val url = buildUrl(grid, base)
            val body = request(url)
            KmaForecastParser.parse(body, targetDateTime)
        }
    }

    private fun buildUrl(grid: KmaGrid, base: KmaBaseTime): String {
        val encodedKey = URLEncoder.encode(serviceKey, Charsets.UTF_8.name())
        return buildString {
            append("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst")
            append("?serviceKey=").append(encodedKey)
            append("&pageNo=1")
            append("&numOfRows=1000")
            append("&dataType=JSON")
            append("&base_date=").append(base.date.format(DateTimeFormatter.BASIC_ISO_DATE))
            append("&base_time=").append(base.time)
            append("&nx=").append(grid.nx)
            append("&ny=").append(grid.ny)
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
            if (code !in 200..299) error("KMA request failed: HTTP $code $body")
            body
        } finally {
            connection.disconnect()
        }
    }

}

object KmaBaseTimeCalculator {
    private val baseTimes = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")

    fun forTarget(
        targetDateTime: LocalDateTime,
        now: LocalDateTime = LocalDateTime.now(),
    ): KmaBaseTime {
        val available = now.minusMinutes(20)
        val baseDate = if (available.toLocalTime() < LocalTime.of(2, 0)) {
            available.toLocalDate().minusDays(1)
        } else {
            available.toLocalDate()
        }
        val time = baseTimes.lastOrNull { LocalTime.parse(it, DateTimeFormatter.ofPattern("HHmm")) <= available.toLocalTime() }
            ?: "2300"
        val date = if (time == "2300" && available.toLocalTime() < LocalTime.of(2, 0)) {
            baseDate
        } else {
            baseDate
        }
        return KmaBaseTime(date, time)
    }
}

data class KmaBaseTime(
    val date: LocalDate,
    val time: String,
)
