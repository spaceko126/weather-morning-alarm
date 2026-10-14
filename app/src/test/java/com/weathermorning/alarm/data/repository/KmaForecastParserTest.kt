package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.WeatherCondition
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class KmaForecastParserTest {
    @Test
    fun parsesTargetHourPopPtyAndPcp() {
        val target = LocalDateTime.of(2026, 6, 17, 7, 30)

        val snapshot = KmaForecastParser.parse(forecastBody(pop = "80", pty = "1", pcp = "2.0mm"), target)

        assertEquals(target, snapshot.targetDateTime)
        assertEquals(WeatherCondition.RAIN, snapshot.condition)
        assertEquals(80, snapshot.precipitationProbability)
        assertEquals(2.0, snapshot.precipitationAmountMm, 0.001)
        assertEquals("kma:getVilageFcst", snapshot.source)
    }

    @Test
    fun ignoresOtherForecastHours() {
        val target = LocalDateTime.of(2026, 6, 17, 7, 30)

        val snapshot = KmaForecastParser.parse(
            forecastBody(pop = "10", pty = "0", otherHourPop = "90", otherHourPty = "3"),
            target,
        )

        assertEquals(WeatherCondition.CLEAR, snapshot.condition)
        assertEquals(10, snapshot.precipitationProbability)
    }

    @Test
    fun mapsSnowAndSleetCodes() {
        val target = LocalDateTime.of(2026, 6, 17, 7, 30)

        assertEquals(
            WeatherCondition.SNOW,
            KmaForecastParser.parse(forecastBody(pop = "60", pty = "3"), target).condition,
        )
        assertEquals(
            WeatherCondition.SLEET,
            KmaForecastParser.parse(forecastBody(pop = "60", pty = "6"), target).condition,
        )
    }

    @Test
    fun parsesKmaTracePrecipitationAsDrizzleAmount() {
        val target = LocalDateTime.of(2026, 6, 17, 7, 30)

        val snapshot = KmaForecastParser.parse(forecastBody(pop = "60", pty = "1", pcp = "1mm 미만"), target)

        assertEquals(0.1, snapshot.precipitationAmountMm, 0.001)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsOnKmaErrorResult() {
        KmaForecastParser.parse(errorBody(), LocalDateTime.of(2026, 6, 17, 7, 30))
    }

    private fun forecastBody(
        pop: String,
        pty: String,
        pcp: String = "강수없음",
        otherHourPop: String = "20",
        otherHourPty: String = "0",
    ): String {
        return """
            {
              "response": {
                "header": {
                  "resultCode": "00",
                  "resultMsg": "NORMAL_SERVICE"
                },
                "body": {
                  "items": {
                    "item": [
                      { "category": "POP", "fcstDate": "20260617", "fcstTime": "0700", "fcstValue": "$pop" },
                      { "category": "PCP", "fcstDate": "20260617", "fcstTime": "0700", "fcstValue": "$pcp" },
                      { "category": "PTY", "fcstDate": "20260617", "fcstTime": "0700", "fcstValue": "$pty" },
                      { "category": "POP", "fcstDate": "20260617", "fcstTime": "0800", "fcstValue": "$otherHourPop" },
                      { "category": "PTY", "fcstDate": "20260617", "fcstTime": "0800", "fcstValue": "$otherHourPty" }
                    ]
                  }
                }
              }
            }
        """.trimIndent()
    }

    private fun errorBody(): String {
        return """
            {
              "response": {
                "header": {
                  "resultCode": "03",
                  "resultMsg": "NO_DATA"
                }
              }
            }
        """.trimIndent()
    }
}
