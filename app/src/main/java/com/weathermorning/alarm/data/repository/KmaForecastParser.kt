package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

object KmaForecastParser {
    fun parse(body: String, targetDateTime: LocalDateTime): WeatherSnapshot {
        val root = JSONObject(body).getJSONObject("response")
        val header = root.getJSONObject("header")
        val resultCode = header.getString("resultCode")
        if (resultCode != "00") {
            error("KMA error ${header.getString("resultCode")}: ${header.getString("resultMsg")}")
        }

        val items = root.getJSONObject("body")
            .getJSONObject("items")
            .getJSONArray("item")

        val targetDate = targetDateTime.format(DateTimeFormatter.BASIC_ISO_DATE)
        val targetHour = "%02d00".format(targetDateTime.hour)
        var precipitationProbability = 0
        var precipitationAmountMm = 0.0
        var condition = WeatherCondition.CLEAR

        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            if (item.getString("fcstDate") != targetDate || item.getString("fcstTime") != targetHour) {
                continue
            }

            when (item.getString("category")) {
                "POP" -> precipitationProbability = item.getString("fcstValue").toIntOrNull() ?: 0
                "PCP" -> precipitationAmountMm = parsePrecipitationAmount(item.getString("fcstValue"))
                "PTY" -> condition = conditionFromPty(item.getString("fcstValue"))
            }
        }

        return WeatherSnapshot(
            checkedAt = Instant.now(),
            targetDateTime = targetDateTime,
            condition = condition,
            precipitationProbability = precipitationProbability,
            precipitationAmountMm = precipitationAmountMm,
            source = "kma:getVilageFcst",
        )
    }

    private fun parsePrecipitationAmount(value: String): Double {
        val trimmed = value.trim()
        if (trimmed == "강수없음" || trimmed == "없음" || trimmed == "-") return 0.0
        if (trimmed.startsWith("1mm") && trimmed.contains("미만")) return 0.1
        return Regex("""\d+(\.\d+)?""").find(trimmed)?.value?.toDoubleOrNull() ?: 0.0
    }

    private fun conditionFromPty(value: String): WeatherCondition {
        return when (value) {
            "1" -> WeatherCondition.RAIN
            "2" -> WeatherCondition.SLEET
            "3" -> WeatherCondition.SNOW
            "5" -> WeatherCondition.RAIN
            "6" -> WeatherCondition.SLEET
            "7" -> WeatherCondition.SNOW
            else -> WeatherCondition.CLEAR
        }
    }
}
