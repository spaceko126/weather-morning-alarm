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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecideWeatherWindowUseCaseTest {
    private val useCase = DecideWeatherWindowUseCase()

    @Test
    fun ringsWhenAnyHourInWindowMatches() {
        val decision = useCase.decide(
            settings = settings(),
            weatherSnapshots = listOf(
                weather(LocalDateTime.of(2026, 6, 17, 6, 0), WeatherCondition.CLEAR, 10),
                weather(LocalDateTime.of(2026, 6, 17, 8, 0), WeatherCondition.RAIN, 40),
            ),
        )

        assertTrue(decision.shouldRing)
        assertEquals(LocalDateTime.of(2026, 6, 17, 8, 0), decision.weatherSnapshot?.targetDateTime)
    }

    @Test
    fun rainInWindowMustMeetPrecipitationThreshold() {
        val decision = useCase.decide(
            settings = settings(precipitationThreshold = 60),
            weatherSnapshots = listOf(
                weather(LocalDateTime.of(2026, 6, 17, 6, 0), WeatherCondition.RAIN, 59),
                weather(LocalDateTime.of(2026, 6, 17, 8, 0), WeatherCondition.RAIN, 60),
            ),
        )

        assertTrue(decision.shouldRing)
        assertEquals(LocalDateTime.of(2026, 6, 17, 8, 0), decision.weatherSnapshot?.targetDateTime)
    }

    @Test
    fun rainInWindowSkipsWhenAllHoursAreBelowPrecipitationThreshold() {
        val decision = useCase.decide(
            settings = settings(precipitationThreshold = 60),
            weatherSnapshots = listOf(
                weather(LocalDateTime.of(2026, 6, 17, 6, 0), WeatherCondition.RAIN, 58),
                weather(LocalDateTime.of(2026, 6, 17, 8, 0), WeatherCondition.RAIN, 59),
            ),
        )

        assertFalse(decision.shouldRing)
        assertEquals(59, decision.weatherSnapshot?.precipitationProbability)
    }

    @Test
    fun skipsWhenNoHourInWindowMatchesAndKeepsStrongestWeather() {
        val decision = useCase.decide(
            settings = settings(precipitationThreshold = 70),
            weatherSnapshots = listOf(
                weather(LocalDateTime.of(2026, 6, 17, 6, 0), WeatherCondition.CLEAR, 10),
                weather(LocalDateTime.of(2026, 6, 17, 8, 0), WeatherCondition.CLEAR, 50),
            ),
        )

        assertFalse(decision.shouldRing)
        assertEquals(50, decision.weatherSnapshot?.precipitationProbability)
    }

    @Test
    fun usesFailPolicyWhenNoForecastSucceeded() {
        val decision = useCase.decide(
            settings = settings(failPolicy = FailPolicy.SKIP),
            weatherSnapshots = emptyList(),
            failure = IllegalStateException("failed"),
        )

        assertFalse(decision.shouldRing)
    }

    @Test
    fun commuteWindowChoosesLaterHourThatMeetsAmountThreshold() {
        val decision = useCase.decide(
            settings = settings(rainAmountThreshold = RainAmountThreshold.LIGHT),
            weatherSnapshots = listOf(
                weather(LocalDateTime.of(2026, 6, 17, 6, 0), WeatherCondition.RAIN, 90, amountMm = 0.1),
                weather(LocalDateTime.of(2026, 6, 17, 8, 0), WeatherCondition.RAIN, 50, amountMm = 2.5),
            ),
        )

        assertTrue(decision.shouldRing)
        assertEquals(LocalDateTime.of(2026, 6, 17, 8, 0), decision.weatherSnapshot?.targetDateTime)
    }

    private fun settings(
        precipitationThreshold: Int = 40,
        rainAmountThreshold: RainAmountThreshold = RainAmountThreshold.NONE,
        failPolicy: FailPolicy = FailPolicy.RING,
    ): AlarmSettings {
        return AlarmSettings(
            enabled = true,
            alarmTime = LocalTime.of(7, 0),
            repeatDays = setOf(DayOfWeek.MONDAY),
            excludePublicHolidays = false,
            locationName = "Seoul",
            latitude = 37.5665,
            longitude = 126.9780,
            triggerRain = true,
            triggerSnow = true,
            precipitationThreshold = precipitationThreshold,
            rainAmountThreshold = rainAmountThreshold,
            failPolicy = failPolicy,
        )
    }

    private fun weather(
        targetDateTime: LocalDateTime,
        condition: WeatherCondition,
        precipitationProbability: Int,
        amountMm: Double = 0.0,
    ): WeatherSnapshot {
        return WeatherSnapshot(
            checkedAt = Instant.parse("2026-06-17T07:00:00Z"),
            targetDateTime = targetDateTime,
            condition = condition,
            precipitationProbability = precipitationProbability,
            precipitationAmountMm = amountMm,
            source = "test",
        )
    }
}
