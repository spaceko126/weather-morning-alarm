package com.weathermorning.alarm.data.repository

import com.weathermorning.alarm.BuildConfig
import com.weathermorning.alarm.data.local.WeatherApiSettings
import com.weathermorning.alarm.data.local.WeatherProvider
import com.weathermorning.alarm.domain.model.AlarmSettings
import com.weathermorning.alarm.domain.model.WeatherSnapshot
import com.weathermorning.alarm.domain.repository.WeatherRepository
import java.time.LocalDateTime

class WeatherRepositoryResolver(
    private val backendProxyRepository: WeatherRepository? = BackendProxyWeatherRepository.fromBuildConfig(),
    private val openMeteoRepository: WeatherRepository = OpenMeteoWeatherRepository(),
    private val stubRepository: WeatherRepository = StubWeatherRepository(),
    private val kmaRepositoryFactory: (String) -> WeatherRepository = { KmaWeatherRepository(it) },
    /**
     * The stub returns deterministic fair weather as a *success*. In a release
     * build that would silently replace a total provider outage with "clear,
     * 10%" and the user's fail policy would never apply, so it stays a debug
     * tool only.
     */
    private val stubFallbackEnabled: Boolean = BuildConfig.DEBUG,
) {
    suspend fun getForecast(
        settings: WeatherApiSettings,
        alarmSettings: AlarmSettings,
        targetDateTime: LocalDateTime,
    ): WeatherResolution {
        val attempts = repositoriesFor(settings)
        var lastFailure: Throwable? = null
        val results = mutableListOf<WeatherProviderAttempt>()

        for (attempt in attempts) {
            val result = attempt.repository.getForecast(alarmSettings, targetDateTime)
            val failure = result.exceptionOrNull()
            results += WeatherProviderAttempt(
                provider = attempt.provider,
                success = result.isSuccess,
                errorMessage = failure?.message,
            )
            if (result.isSuccess) {
                return WeatherResolution(
                    result = result,
                    attempts = results,
                )
            }
            lastFailure = result.exceptionOrNull()
        }

        return WeatherResolution(
            result = Result.failure(lastFailure ?: IllegalStateException("No weather provider available")),
            attempts = results,
        )
    }

    internal fun providersFor(settings: WeatherApiSettings): List<WeatherProvider> {
        return repositoriesFor(settings).map { it.provider }
    }

    private fun repositoriesFor(settings: WeatherApiSettings): List<RepositoryAttempt> {
        return when (settings.provider) {
            WeatherProvider.BACKEND_PROXY -> buildList {
                addBackendProxyIfConfigured()
                add(RepositoryAttempt(WeatherProvider.OPEN_METEO, openMeteoRepository))
                addKmaIfConfigured(settings)
                addStubIfEnabled()
            }
            WeatherProvider.OPEN_METEO -> buildList {
                add(RepositoryAttempt(WeatherProvider.OPEN_METEO, openMeteoRepository))
                addKmaIfConfigured(settings)
                addStubIfEnabled()
            }
            WeatherProvider.KMA -> buildList {
                addKmaIfConfigured(settings)
                add(RepositoryAttempt(WeatherProvider.OPEN_METEO, openMeteoRepository))
                addStubIfEnabled()
            }
            WeatherProvider.STUB -> buildList {
                addStubIfEnabled()
                if (isEmpty()) {
                    add(RepositoryAttempt(WeatherProvider.OPEN_METEO, openMeteoRepository))
                }
            }
        }
    }

    private fun MutableList<RepositoryAttempt>.addBackendProxyIfConfigured() {
        val repository = backendProxyRepository ?: return
        add(RepositoryAttempt(WeatherProvider.BACKEND_PROXY, repository))
    }

    private fun MutableList<RepositoryAttempt>.addKmaIfConfigured(settings: WeatherApiSettings) {
        if (settings.kmaServiceKey.isBlank()) return
        add(RepositoryAttempt(WeatherProvider.KMA, kmaRepositoryFactory(settings.kmaServiceKey)))
    }

    private fun MutableList<RepositoryAttempt>.addStubIfEnabled() {
        if (!stubFallbackEnabled) return
        add(RepositoryAttempt(WeatherProvider.STUB, stubRepository))
    }

    private data class RepositoryAttempt(
        val provider: WeatherProvider,
        val repository: WeatherRepository,
    )
}

data class WeatherResolution(
    val result: Result<WeatherSnapshot>,
    val attempts: List<WeatherProviderAttempt>,
) {
    val failedAttemptsBeforeSuccess: List<WeatherProviderAttempt>
        get() = if (result.isSuccess) attempts.dropLast(1).filter { !it.success } else emptyList()

    val successfulProvider: WeatherProvider?
        get() = attempts.lastOrNull { it.success }?.provider
}

data class WeatherProviderAttempt(
    val provider: WeatherProvider,
    val success: Boolean,
    val errorMessage: String?,
)
