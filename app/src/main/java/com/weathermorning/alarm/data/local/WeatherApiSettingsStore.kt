package com.weathermorning.alarm.data.local

import android.content.Context
import androidx.core.content.edit
import com.weathermorning.alarm.BuildConfig

class WeatherApiSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("weather_api_settings", Context.MODE_PRIVATE)

    fun load(): WeatherApiSettings {
        val defaultProvider = WeatherProviderDefaults.defaultProvider
        return WeatherApiSettings(
            provider = runCatching {
                WeatherProvider.valueOf(
                    prefs.getString("provider", defaultProvider.name)
                        ?: defaultProvider.name,
                )
            }.getOrDefault(defaultProvider),
            kmaServiceKey = prefs.getString("kma_service_key", "").orEmpty(),
        )
    }

    fun save(settings: WeatherApiSettings) {
        prefs.edit {
            putString("provider", settings.provider.name)
            putString("kma_service_key", settings.kmaServiceKey)
        }
    }
}

data class WeatherApiSettings(
    val provider: WeatherProvider,
    val kmaServiceKey: String,
)

enum class WeatherProvider {
    BACKEND_PROXY,
    OPEN_METEO,
    STUB,
    KMA,
}

object WeatherProviderDefaults {
    val backendProxyEnabled: Boolean
        get() = BuildConfig.WEATHER_PROXY_BASE_URL.isNotBlank()

    val defaultProvider: WeatherProvider
        get() = if (backendProxyEnabled) WeatherProvider.BACKEND_PROXY else WeatherProvider.OPEN_METEO
}
