package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.domain.repository.WeatherRepository
import java.time.Instant
import java.time.LocalDateTime

class StubWeatherRepository : WeatherRepository {
    override suspend fun getForecast(
        settings: AlarmSettings,
        targetDateTime: LocalDateTime,
    ): Result<WeatherSnapshot> {
        return Result.success(
            WeatherSnapshot(
                checkedAt = Instant.now(),
                targetDateTime = targetDateTime,
                condition = WeatherCondition.CLEAR,
                precipitationProbability = 10,
                precipitationAmountMm = 0.0,
                source = "stub",
            ),
        )
    }
}
