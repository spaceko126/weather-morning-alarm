package com.weathermorning.alarm.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.weathermorning.alarm.alarm.AndroidAlarmScheduler
import com.weathermorning.alarm.data.local.LocalAlarmSettingsStore
import com.weathermorning.alarm.data.local.WeatherApiSettings
import com.weathermorning.alarm.data.local.WeatherApiSettingsStore
import com.weathermorning.alarm.domain.model.AlarmSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmSettingsStore = LocalAlarmSettingsStore(application)
    private val weatherApiSettingsStore = WeatherApiSettingsStore(application)
    private val alarmScheduler = AndroidAlarmScheduler(application)

    private val _alarms = MutableStateFlow(alarmSettingsStore.loadAll())
    val alarms: StateFlow<List<AlarmSettings>> = _alarms.asStateFlow()

    private val _weatherApiSettings = MutableStateFlow(weatherApiSettingsStore.load())
    val weatherApiSettings: StateFlow<WeatherApiSettings> = _weatherApiSettings.asStateFlow()

    fun save(alarm: AlarmSettings) {
        alarmSettingsStore.save(alarm)
        alarmScheduler.scheduleNext(alarm)
        _alarms.value = alarmSettingsStore.loadAll()
    }

    /**
     * Applies [transform] to the alarm as it is *now*. Callbacks that arrive
     * later — geocoding, the ringtone picker — must use this rather than copying
     * a snapshot captured when they were registered, or they silently revert
     * whatever the user changed while they were running.
     */
    fun update(id: Long, transform: (AlarmSettings) -> AlarmSettings) {
        val current = _alarms.value.firstOrNull { it.id == id } ?: return
        save(transform(current))
    }

    /** Adds an alarm seeded from the most recent one's weather preferences. */
    fun addAlarm(): Long {
        val template = _alarms.value.lastOrNull() ?: AlarmSettings.Default
        val alarm = template.copy(
            id = alarmSettingsStore.nextId(),
            label = "",
            enabled = true,
        )
        save(alarm)
        return alarm.id
    }

    fun delete(id: Long) {
        alarmScheduler.forget(id)
        alarmSettingsStore.delete(id)
        _alarms.value = alarmSettingsStore.loadAll()
    }

    fun updateWeatherApiSettings(settings: WeatherApiSettings) {
        weatherApiSettingsStore.save(settings)
        _weatherApiSettings.value = settings
    }
}
