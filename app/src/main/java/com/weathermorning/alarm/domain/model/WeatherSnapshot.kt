package com.weathermorning.alarm.domain.model

import java.time.Instant
import java.time.LocalDateTime

data class WeatherSnapshot(
    val checkedAt: Instant,
    val targetDateTime: LocalDateTime,
    val condition: WeatherCondition,
    val precipitationProbability: Int,
    val precipitationAmountMm: Double = 0.0,
    val source: String,
)

enum class WeatherCondition {
    CLEAR,
    RAIN,
    SNOW,
    SLEET,
    UNKNOWN,
}
