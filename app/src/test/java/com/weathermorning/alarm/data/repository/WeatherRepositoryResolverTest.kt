package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.data.local.WeatherApiSettings
import com.weathermorning.alarm.data.local.WeatherProvider
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.FailPolicy
import com.weathermorning.alarm.domain.model.WeatherCondition
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.domain.repository.WeatherRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryResolverTest {
    @Test
    fun openMeteoFallsBackToKmaThenStubWhenKeyExists() {
        val resolver = WeatherRepositoryResolver()

        assertEquals(
            listOf(WeatherProvider.OPEN_METEO, WeatherProvider.KMA, WeatherProvider.STUB),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.OPEN_METEO, "key")),
        )
    }

    @Test
    fun openMeteoFallsBackToStubWhenKmaKeyMissing() {
        val resolver = WeatherRepositoryResolver()

        assertEquals(
            listOf(WeatherProvider.OPEN_METEO, WeatherProvider.STUB),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.OPEN_METEO, "")),
        )
    }

    @Test
    fun backendProxyFallsBackToLocalProvidersWhenConfigured() {
        val resolver = WeatherRepositoryResolver(
            backendProxyRepository = FakeRepository(Result.failure(IllegalStateException("proxy failed"))),
        )

        assertEquals(
            listOf(WeatherProvider.BACKEND_PROXY, WeatherProvider.OPEN_METEO, WeatherProvider.KMA, WeatherProvider.STUB),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.BACKEND_PROXY, "key")),
        )
    }

    @Test
    fun backendProxySelectionSkipsProxyWhenNotConfigured() {
        val resolver = WeatherRepositoryResolver(backendProxyRepository = null)

        assertEquals(
            listOf(WeatherProvider.OPEN_METEO, WeatherProvider.STUB),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.BACKEND_PROXY, "")),
        )
    }

    @Test
    fun kmaFallsBackToOpenMeteoThenStub() {
        val resolver = WeatherRepositoryResolver()

        assertEquals(
            listOf(WeatherProvider.KMA, WeatherProvider.OPEN_METEO, WeatherProvider.STUB),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.KMA, "key")),
        )
    }

    @Test
    fun returnsFirstSuccessfulFallbackSnapshot() = runBlocking {
        val openMeteo = FakeRepository(Result.failure(IllegalStateException("open failed")))
        val kma = FakeRepository(Result.failure(IllegalStateException("kma failed")))
        val stub = FakeRepository(Result.success(snapshot(source = "stub-test")))
        val resolver = WeatherRepositoryResolver(
            openMeteoRepository = openMeteo,
            stubRepository = stub,
            kmaRepositoryFactory = { kma },
        )

        val resolution = resolver.getForecast(
            settings = WeatherApiSettings(WeatherProvider.OPEN_METEO, "key"),
            alarmSettings = settings(),
            targetDateTime = LocalDateTime.of(2026, 6, 19, 7, 0),
        )

        assertTrue(resolution.result.isSuccess)
        assertEquals("stub-test", resolution.result.getOrThrow().source)
        assertEquals(
            listOf(WeatherProvider.OPEN_METEO, WeatherProvider.KMA),
            resolution.failedAttemptsBeforeSuccess.map { it.provider },
        )
        assertEquals(WeatherProvider.STUB, resolution.successfulProvider)
        assertEquals(1, openMeteo.calls)
        assertEquals(1, kma.calls)
        assertEquals(1, stub.calls)
    }

    @Test
    fun returnsLastFailureWhenEveryProviderFails() = runBlocking {
        val resolver = WeatherRepositoryResolver(
            openMeteoRepository = FakeRepository(Result.failure(IllegalStateException("open failed"))),
            stubRepository = FakeRepository(Result.failure(IllegalStateException("stub failed"))),
        )

        val resolution = resolver.getForecast(
            settings = WeatherApiSettings(WeatherProvider.OPEN_METEO, ""),
            alarmSettings = settings(),
            targetDateTime = LocalDateTime.of(2026, 6, 19, 7, 0),
        )

        assertTrue(resolution.result.isFailure)
        assertEquals("stub failed", resolution.result.exceptionOrNull()?.message)
        assertEquals(
            listOf(WeatherProvider.OPEN_METEO, WeatherProvider.STUB),
            resolution.attempts.map { it.provider },
        )
    }

    @Test
    fun releaseBuildsDropStubSoFailPolicyCanApply() {
        val resolver = WeatherRepositoryResolver(stubFallbackEnabled = false)

        assertEquals(
            listOf(WeatherProvider.OPEN_METEO, WeatherProvider.KMA),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.OPEN_METEO, "key")),
        )
    }

    @Test
    fun releaseBuildsSurfaceFailureWhenEveryRealProviderFails() = runBlocking {
        val resolver = WeatherRepositoryResolver(
            openMeteoRepository = FakeRepository(Result.failure(IllegalStateException("open failed"))),
            stubRepository = FakeRepository(Result.success(snapshot(source = "stub-test"))),
            stubFallbackEnabled = false,
        )

        val resolution = resolver.getForecast(
            settings = WeatherApiSettings(WeatherProvider.OPEN_METEO, ""),
            alarmSettings = settings(),
            targetDateTime = LocalDateTime.of(2026, 6, 19, 7, 0),
        )

        assertTrue(resolution.result.isFailure)
        assertEquals(listOf(WeatherProvider.OPEN_METEO), resolution.attempts.map { it.provider })
    }

    @Test
    fun stubSelectionFallsBackToOpenMeteoInReleaseBuilds() {
        val resolver = WeatherRepositoryResolver(stubFallbackEnabled = false)

        assertEquals(
            listOf(WeatherProvider.OPEN_METEO),
            resolver.providersFor(WeatherApiSettings(WeatherProvider.STUB, "")),
        )
    }

    private class FakeRepository(
        private val result: Result<WeatherSnapshot>,
    ) : WeatherRepository {
        var calls = 0

        override suspend fun getForecast(
            settings: AlarmSettings,
            targetDateTime: LocalDateTime,
        ): Result<WeatherSnapshot> {
            calls += 1
            return result
        }
    }

    private fun settings(): AlarmSettings {
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
            precipitationThreshold = 40,
            failPolicy = FailPolicy.RING,
        )
    }

    private fun snapshot(source: String): WeatherSnapshot {
        return WeatherSnapshot(
            checkedAt = Instant.parse("2026-06-18T00:00:00Z"),
            targetDateTime = LocalDateTime.of(2026, 6, 19, 7, 0),
            condition = WeatherCondition.CLEAR,
            precipitationProbability = 10,
            source = source,
        )
    }
}
