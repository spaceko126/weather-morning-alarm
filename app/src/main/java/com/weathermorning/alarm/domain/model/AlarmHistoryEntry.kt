package com.weathermorning.alarm.domain.model

import java.time.Instant

data class AlarmHistoryEntry(
    val id: String,
    val occurredAt: Instant,
    val type: AlarmHistoryType,
    val reason: String,
    val reasonCode: AlarmReasonCode?,
    val reasonCondition: WeatherCondition?,
    val reasonValue1: Int?,
    val reasonValue2: Int?,
    val weatherCondition: WeatherCondition?,
    val precipitationProbability: Int?,
    val precipitationAmountMm: Double?,
    val source: String?,
    val alarmId: Long? = null,
)

enum class AlarmHistoryType {
    PRECHECK,
    RING,
    SKIP,
    TEST,
    DIAGNOSTIC,
}
