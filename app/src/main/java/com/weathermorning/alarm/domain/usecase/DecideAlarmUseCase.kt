package com.weathermorning.alarm.domain.usecase

import com.weathermorning.alarm.domain.model.AlarmDecision
import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.FailPolicy
import com.weathermorning.alarm.domain.model.RainAmountThreshold
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.Instant

class DecideAlarmUseCase {
    fun decide(
        settings: AlarmSettings,
        weather: WeatherSnapshot?,
        failure: Throwable? = null,
    ): AlarmDecision {
        if (!settings.enabled) {
            return AlarmDecision(
                shouldRing = false,
                reason = "Alarm disabled",
                decidedAt = Instant.now(),
                weatherSnapshot = weather,
                reasonCode = AlarmReasonCode.ALARM_DISABLED,
            )
        }

        if (failure != null || weather == null) {
            val shouldRing = settings.failPolicy == FailPolicy.RING
            val reason = if (shouldRing) {
                "Weather check failed, fail policy is ring"
            } else {
                "Weather check failed, fail policy is skip"
            }
            return AlarmDecision(
                shouldRing = shouldRing,
                reason = reason,
                decidedAt = Instant.now(),
                weatherSnapshot = weather,
                reasonCode = if (shouldRing) {
                    AlarmReasonCode.WEATHER_CHECK_FAILED_RING
                } else {
                    AlarmReasonCode.WEATHER_CHECK_FAILED_SKIP
                },
            )
        }

        val probabilityMatches = weather.precipitationProbability >= settings.precipitationThreshold
        val amountMatches = precipitationAmountMatches(settings.rainAmountThreshold, weather)

        if (settings.triggerRain && weather.condition == WeatherCondition.RAIN && probabilityMatches && amountMatches) {
            return AlarmDecision(
                shouldRing = true,
                reason = rainReason("Rain forecast", settings, weather),
                decidedAt = Instant.now(),
                weatherSnapshot = weather,
                reasonCode = AlarmReasonCode.RAIN_FORECAST,
            )
        }

        if (settings.triggerSnow && weather.condition == WeatherCondition.SNOW && probabilityMatches && amountMatches) {
            return AlarmDecision(
                shouldRing = true,
                reason = rainReason("Snow forecast", settings, weather),
                decidedAt = Instant.now(),
                weatherSnapshot = weather,
                reasonCode = AlarmReasonCode.SNOW_FORECAST,
            )
        }

        if (
            (settings.triggerRain || settings.triggerSnow) &&
            weather.condition == WeatherCondition.SLEET &&
            probabilityMatches &&
            amountMatches
        ) {
            return AlarmDecision(
                shouldRing = true,
                reason = rainReason("Sleet forecast", settings, weather),
                decidedAt = Instant.now(),
                weatherSnapshot = weather,
                reasonCode = AlarmReasonCode.SLEET_FORECAST,
            )
        }

        if (probabilityMatches && amountMatches) {
            return AlarmDecision(
                shouldRing = true,
                reason = precipitationReason(settings, weather),
                decidedAt = Instant.now(),
                weatherSnapshot = weather,
                reasonCode = AlarmReasonCode.PRECIPITATION_THRESHOLD,
                reasonValue1 = weather.precipitationProbability,
                reasonValue2 = settings.precipitationThreshold,
            )
        }

        return AlarmDecision(
            shouldRing = false,
            reason = noTriggerReason(settings, weather),
            decidedAt = Instant.now(),
            weatherSnapshot = weather,
            reasonCode = AlarmReasonCode.NO_TRIGGER,
            reasonCondition = weather.condition,
            reasonValue1 = weather.precipitationProbability,
        )
    }

    private fun precipitationAmountMatches(
        threshold: RainAmountThreshold,
        weather: WeatherSnapshot,
    ): Boolean {
        return threshold == RainAmountThreshold.NONE ||
            weather.precipitationAmountMm >= threshold.minMmPerHour
    }

    private fun rainReason(
        base: String,
        settings: AlarmSettings,
        weather: WeatherSnapshot,
    ): String {
        if (settings.rainAmountThreshold == RainAmountThreshold.NONE) return base
        return "$base, precipitation amount ${weather.precipitationAmountMm}mm >= ${settings.rainAmountThreshold.minMmPerHour}mm"
    }

    private fun precipitationReason(
        settings: AlarmSettings,
        weather: WeatherSnapshot,
    ): String {
        val probability = "Precipitation probability ${weather.precipitationProbability}% >= ${settings.precipitationThreshold}%"
        if (settings.rainAmountThreshold == RainAmountThreshold.NONE) return probability
        return "$probability, amount ${weather.precipitationAmountMm}mm >= ${settings.rainAmountThreshold.minMmPerHour}mm"
    }

    private fun noTriggerReason(
        settings: AlarmSettings,
        weather: WeatherSnapshot,
    ): String {
        val base = "No trigger: ${weather.condition}, precipitation ${weather.precipitationProbability}%"
        if (settings.rainAmountThreshold == RainAmountThreshold.NONE) return base
        return "$base, amount ${weather.precipitationAmountMm}mm < ${settings.rainAmountThreshold.minMmPerHour}mm"
    }
}
