package com.weathermorning.alarm.presentation.history

import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.WeatherCondition

internal sealed interface HistoryReasonDisplay {
    data object AlarmDisabled : HistoryReasonDisplay
    data object WeatherCheckFailedRing : HistoryReasonDisplay
    data object WeatherCheckFailedSkip : HistoryReasonDisplay
    data object RainForecast : HistoryReasonDisplay
    data object SnowForecast : HistoryReasonDisplay
    data object SleetForecast : HistoryReasonDisplay
    data object ReceiverTestAlarm : HistoryReasonDisplay
    data object WeatherConditionMet : HistoryReasonDisplay
    data object NoSavedDecisionFailRing : HistoryReasonDisplay
    data object WeatherConditionNotMet : HistoryReasonDisplay
    data class PrecipitationThreshold(val actual: Int, val threshold: Int) : HistoryReasonDisplay
    data class NoTrigger(val condition: WeatherCondition, val probability: Int) : HistoryReasonDisplay
    data class Raw(val text: String) : HistoryReasonDisplay
}

internal object HistoryReasonDisplayParser {
    fun parseCode(
        code: AlarmReasonCode,
        condition: WeatherCondition?,
        value1: Int?,
        value2: Int?,
    ): HistoryReasonDisplay {
        return when (code) {
            AlarmReasonCode.ALARM_DISABLED -> HistoryReasonDisplay.AlarmDisabled
            AlarmReasonCode.WEATHER_CHECK_FAILED_RING -> HistoryReasonDisplay.WeatherCheckFailedRing
            AlarmReasonCode.WEATHER_CHECK_FAILED_SKIP -> HistoryReasonDisplay.WeatherCheckFailedSkip
            AlarmReasonCode.RAIN_FORECAST -> HistoryReasonDisplay.RainForecast
            AlarmReasonCode.SNOW_FORECAST -> HistoryReasonDisplay.SnowForecast
            AlarmReasonCode.SLEET_FORECAST -> HistoryReasonDisplay.SleetForecast
            AlarmReasonCode.RECEIVER_TEST_ALARM -> HistoryReasonDisplay.ReceiverTestAlarm
            AlarmReasonCode.WEATHER_CONDITION_MET -> HistoryReasonDisplay.WeatherConditionMet
            AlarmReasonCode.NO_SAVED_DECISION_FAIL_RING -> HistoryReasonDisplay.NoSavedDecisionFailRing
            AlarmReasonCode.WEATHER_CONDITION_NOT_MET -> HistoryReasonDisplay.WeatherConditionNotMet
            AlarmReasonCode.PRECIPITATION_THRESHOLD -> {
                if (value1 != null && value2 != null) {
                    HistoryReasonDisplay.PrecipitationThreshold(value1, value2)
                } else {
                    HistoryReasonDisplay.Raw(code.name)
                }
            }
            AlarmReasonCode.NO_TRIGGER -> {
                if (condition != null && value1 != null) {
                    HistoryReasonDisplay.NoTrigger(condition, value1)
                } else {
                    HistoryReasonDisplay.Raw(code.name)
                }
            }
        }
    }

    fun parse(reason: String): HistoryReasonDisplay {
        return when (reason) {
            "Alarm disabled" -> HistoryReasonDisplay.AlarmDisabled
            "Weather check failed, fail policy is ring" -> HistoryReasonDisplay.WeatherCheckFailedRing
            "Weather check failed, fail policy is skip" -> HistoryReasonDisplay.WeatherCheckFailedSkip
            "Rain forecast" -> HistoryReasonDisplay.RainForecast
            "Snow forecast" -> HistoryReasonDisplay.SnowForecast
            "Sleet forecast" -> HistoryReasonDisplay.SleetForecast
            "Receiver test alarm" -> HistoryReasonDisplay.ReceiverTestAlarm
            "Weather condition met" -> HistoryReasonDisplay.WeatherConditionMet
            "No saved weather decision, fail policy ring" -> HistoryReasonDisplay.NoSavedDecisionFailRing
            "Weather condition not met" -> HistoryReasonDisplay.WeatherConditionNotMet
            else -> precipitationThresholdReason(reason)
                ?: noTriggerReason(reason)
                ?: HistoryReasonDisplay.Raw(reason)
        }
    }

    private fun precipitationThresholdReason(reason: String): HistoryReasonDisplay? {
        val match = PrecipitationThresholdReasonRegex.matchEntire(reason) ?: return null
        val actual = match.groupValues[1].toIntOrNull() ?: return HistoryReasonDisplay.Raw(reason)
        val threshold = match.groupValues[2].toIntOrNull() ?: return HistoryReasonDisplay.Raw(reason)
        return HistoryReasonDisplay.PrecipitationThreshold(actual, threshold)
    }

    private fun noTriggerReason(reason: String): HistoryReasonDisplay? {
        val match = NoTriggerReasonRegex.matchEntire(reason) ?: return null
        val condition = runCatching { WeatherCondition.valueOf(match.groupValues[1]) }.getOrNull()
            ?: return HistoryReasonDisplay.Raw(reason)
        val probability = match.groupValues[2].toIntOrNull() ?: return HistoryReasonDisplay.Raw(reason)
        return HistoryReasonDisplay.NoTrigger(condition, probability)
    }

    fun hasWeatherDetails(reason: String): Boolean {
        return PrecipitationThresholdReasonRegex.matches(reason) ||
            NoTriggerReasonRegex.matches(reason)
    }

    private val PrecipitationThresholdReasonRegex = Regex("""Precipitation probability (\d+)% >= (\d+)%""")
    private val NoTriggerReasonRegex = Regex("""No trigger: ([A-Z]+), precipitation (\d+)%""")
}
