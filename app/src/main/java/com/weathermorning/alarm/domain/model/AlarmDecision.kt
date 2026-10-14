package com.weathermorning.alarm.domain.model

import java.time.Instant

data class AlarmDecision(
    val shouldRing: Boolean,
    val reason: String,
    val decidedAt: Instant,
    val weatherSnapshot: WeatherSnapshot?,
    val weatherSnapshots: List<WeatherSnapshot> = emptyList(),
    val reasonCode: AlarmReasonCode? = null,
    val reasonCondition: WeatherCondition? = null,
    val reasonValue1: Int? = null,
    val reasonValue2: Int? = null,
)
