package com.weathermorning.alarm.domain.usecase

import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.FailPolicy
import com.weathermorning.alarm.domain.model.RainAmountThreshold
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecideAlarmUseCaseTest {
    private val useCase = DecideAlarmUseCase()

    @Test
    fun disabledAlarmSkips() {
        val decision = useCase.decide(settings(enabled = false), weather(WeatherCondition.RAIN, 90))

        assertFalse(decision.shouldRing)
    }

    @Test
    fun rainTriggerRingsOnRainWhenProbabilityMeetsThreshold() {
        val decision = useCase.decide(
            settings(triggerRain = true, precipitationThreshold = 60),
            weather(WeatherCondition.RAIN, 60),
        )

        assertTrue(decision.shouldRing)
    }

    @Test
    fun rainTriggerSkipsWhenProbabilityIsBelowThreshold() {
        val decision = useCase.decide(
            settings(triggerRain = true, precipitationThreshold = 60),
            weather(WeatherCondition.RAIN, 59),
        )

        assertFalse(decision.shouldRing)
    }

    @Test
    fun snowTriggerRingsOnSnowWhenProbabilityMeetsThreshold() {
        val decision = useCase.decide(
            settings(triggerSnow = true, precipitationThreshold = 60),
            weather(WeatherCondition.SNOW, 60),
        )

        assertTrue(decision.shouldRing)
    }

    @Test
    fun precipitationThresholdRingsEvenWhenDirectTriggersOff() {
        val decision = useCase.decide(
            settings(triggerRain = false, triggerSnow = false, precipitationThreshold = 40),
            weather(WeatherCondition.CLEAR, 60, amountMm = 0.0),
        )

        assertTrue(decision.shouldRing)
    }

    @Test
    fun rainAmountThresholdFiltersDirectRainTrigger() {
        val decision = useCase.decide(
            settings(triggerRain = true, rainAmountThreshold = RainAmountThreshold.LIGHT),
            weather(WeatherCondition.RAIN, 90, amountMm = 0.5),
        )

        assertFalse(decision.shouldRing)
    }

    @Test
    fun rainAmountThresholdAllowsRainAtSelectedIntensityOrHigher() {
        val decision = useCase.decide(
            settings(triggerRain = true, rainAmountThreshold = RainAmountThreshold.LIGHT),
            weather(WeatherCondition.RAIN, 40, amountMm = 2.0),
        )

        assertTrue(decision.shouldRing)
    }

    @Test
    fun precipitationThresholdRequiresAmountWhenAmountThresholdSelected() {
        val decision = useCase.decide(
            settings(
                triggerRain = false,
                triggerSnow = false,
                precipitationThreshold = 60,
                rainAmountThreshold = RainAmountThreshold.LIGHT,
            ),
            weather(WeatherCondition.CLEAR, 80, amountMm = 0.2),
        )

        assertFalse(decision.shouldRing)
    }

    @Test
    fun lowPrecipitationSkipsWhenNoConditionMatches() {
        val decision = useCase.decide(
            settings(precipitationThreshold = 70),
            weather(WeatherCondition.CLEAR, 20),
        )

        assertFalse(decision.shouldRing)
    }

    @Test
    fun failurePolicyRingRingsWhenWeatherMissing() {
        val decision = useCase.decide(settings(failPolicy = FailPolicy.RING), weather = null)

        assertTrue(decision.shouldRing)
    }

    @Test
    fun failurePolicySkipSkipsWhenWeatherMissing() {
        val decision = useCase.decide(settings(failPolicy = FailPolicy.SKIP), weather = null)

        assertFalse(decision.shouldRing)
    }

    private fun settings(
        enabled: Boolean = true,
        triggerRain: Boolean = true,
        triggerSnow: Boolean = true,
        precipitationThreshold: Int = 40,
        rainAmountThreshold: RainAmountThreshold = RainAmountThreshold.NONE,
        failPolicy: FailPolicy = FailPolicy.RING,
    ): AlarmSettings {
        return AlarmSettings(
            enabled = enabled,
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.MONDAY),
            excludePublicHolidays = false,
            locationName = "Seoul",
            latitude = 37.5665,
            longitude = 126.9780,
            triggerRain = triggerRain,
            triggerSnow = triggerSnow,
            precipitationThreshold = precipitationThreshold,
            rainAmountThreshold = rainAmountThreshold,
            failPolicy = failPolicy,
        )
    }

    private fun weather(
        condition: WeatherCondition,
        precipitationProbability: Int,
        amountMm: Double = 0.0,
    ): WeatherSnapshot {
        return WeatherSnapshot(
            checkedAt = Instant.parse("2026-06-17T07:00:00Z"),
            targetDateTime = LocalDateTime.of(2026, 6, 17, 7, 0),
            condition = condition,
            precipitationProbability = precipitationProbability,
            precipitationAmountMm = amountMm,
            source = "test",
        )
    }
}
