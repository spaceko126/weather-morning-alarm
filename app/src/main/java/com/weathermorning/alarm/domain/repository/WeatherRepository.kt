package com.weathermorning.alarm.domain.repository

import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.LocalDateTime

interface WeatherRepository {
    suspend fun getForecast(
        settings: AlarmSettings,
        targetDateTime: LocalDateTime,
    ): Result<WeatherSnapshot>
}

