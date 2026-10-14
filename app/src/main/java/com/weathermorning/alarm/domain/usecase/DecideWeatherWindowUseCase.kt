package com.weathermorning.alarm.domain.usecase

import com.weathermorning.alarm.domain.model.AlarmDecision
import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.Instant

class DecideWeatherWindowUseCase(
    private val singleHourDecision: DecideAlarmUseCase = DecideAlarmUseCase(),
) {
    fun decide(
        settings: AlarmSettings,
        weatherSnapshots: List<WeatherSnapshot>,
        failure: Throwable? = null,
    ): AlarmDecision {
        if (weatherSnapshots.isEmpty()) {
            return singleHourDecision.decide(settings, weather = null, failure = failure)
        }

        val decisions = weatherSnapshots
            .sortedBy { it.targetDateTime }
            .map { singleHourDecision.decide(settings, it) }
        val ringDecision = decisions.firstOrNull { it.shouldRing }
        if (ringDecision != null) return ringDecision

        val strongestWeather = weatherSnapshots.maxByOrNull { it.precipitationProbability }
            ?: weatherSnapshots.last()
        return AlarmDecision(
            shouldRing = false,
            reason = "No trigger in weather window: ${strongestWeather.condition}, max precipitation ${strongestWeather.precipitationProbability}%",
            decidedAt = Instant.now(),
            weatherSnapshot = strongestWeather,
            reasonCode = AlarmReasonCode.NO_TRIGGER,
            reasonCondition = strongestWeather.condition,
            reasonValue1 = strongestWeather.precipitationProbability,
        )
    }
}
