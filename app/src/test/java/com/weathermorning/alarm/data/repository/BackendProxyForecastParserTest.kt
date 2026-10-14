package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.WeatherCondition
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendProxyForecastParserTest {
    @Test
    fun parsesSuccessfulForecastResponse() {
        val snapshot = BackendProxyForecastParser.parse(
            """
            {
              "checkedAt": "2026-06-18T12:10:00Z",
              "targetDateTime": "2026-06-19T07:00:00",
              "condition": "RAIN",
              "precipitationProbability": 65,
              "precipitationAmountMm": 3.5,
              "source": "open-meteo:forecast"
            }
            """.trimIndent(),
        )

        assertEquals(Instant.parse("2026-06-18T12:10:00Z"), snapshot.checkedAt)
        assertEquals(LocalDateTime.of(2026, 6, 19, 7, 0), snapshot.targetDateTime)
        assertEquals(WeatherCondition.RAIN, snapshot.condition)
        assertEquals(65, snapshot.precipitationProbability)
        assertEquals(3.5, snapshot.precipitationAmountMm, 0.001)
        assertEquals("backend-proxy:open-meteo:forecast", snapshot.source)
    }

    @Test
    fun usesFallbackSourceWhenSourceIsMissing() {
        val snapshot = BackendProxyForecastParser.parse(
            """
            {
              "checkedAt": "2026-06-18T12:10:00Z",
              "targetDateTime": "2026-06-19T07:00:00",
              "condition": "CLEAR",
              "precipitationProbability": 5
            }
            """.trimIndent(),
        )

        assertEquals("backend-proxy:forecast", snapshot.source)
    }
}
