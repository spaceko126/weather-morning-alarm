package com.weathermorning.alarm.alarm

import com.weathermorning.alarm.domain.model.AlarmSettings

interface AlarmScheduler {
    fun scheduleNext(settings: AlarmSettings)
    fun cancel()
}

