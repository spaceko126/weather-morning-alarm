package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.WeatherCondition
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoForecastParserTest {
    @Test
    fun parsesTargetHourProbabilityAndRainCode() {
        val target = LocalDateTime.of(2026, 6, 19, 7, 0)

        val snapshot = OpenMeteoForecastParser.parse(
            forecastBody(targetCode = 61, targetProbability = 70),
            target,
        )

        assertEquals(target, snapshot.targetDateTime)
        assertEquals(WeatherCondition.RAIN, snapshot.condition)
        assertEquals(70, snapshot.precipitationProbability)
        assertEquals(2.5, snapshot.precipitationAmountMm, 0.001)
        assertEquals("open-meteo:forecast", snapshot.source)
    }

    @Test
    fun mapsSnowAndSleetCodes() {
        val target = LocalDateTime.of(2026, 6, 19, 7, 0)

        assertEquals(
            WeatherCondition.SNOW,
            OpenMeteoForecastParser.parse(forecastBody(targetCode = 71, targetProbability = 60), target).condition,
        )
        assertEquals(
            WeatherCondition.SLEET,
            OpenMeteoForecastParser.parse(forecastBody(targetCode = 66, targetProbability = 60), target).condition,
        )
    }

    @Test
    fun ignoresOtherHours() {
        val target = LocalDateTime.of(2026, 6, 19, 7, 0)

        val snapshot = OpenMeteoForecastParser.parse(
            forecastBody(otherCode = 71, otherProbability = 90, targetCode = 0, targetProbability = 10),
            target,
        )

        assertEquals(WeatherCondition.CLEAR, snapshot.condition)
        assertEquals(10, snapshot.precipitationProbability)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsOnOpenMeteoError() {
        OpenMeteoForecastParser.parse(
            """{"error":true,"reason":"bad hourly variable"}""",
            LocalDateTime.of(2026, 6, 19, 7, 0),
        )
    }

    private fun forecastBody(
        otherCode: Int = 0,
        otherProbability: Int = 20,
        targetCode: Int,
        targetProbability: Int,
    ): String {
        return """
            {
              "hourly": {
                "time": ["2026-06-19T06:00", "2026-06-19T07:00"],
                "precipitation_probability": [$otherProbability, $targetProbability],
                "precipitation": [0.0, 2.5],
                "weather_code": [$otherCode, $targetCode]
              }
            }
        """.trimIndent()
    }
}
