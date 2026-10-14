package com.weathermorning.alarm.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_history")
data class AlarmHistoryEntity(
    @PrimaryKey val id: String,
    val occurredAtEpochMillis: Long,
    val type: String,
    val reason: String,
    val reasonCode: String?,
    val reasonCondition: String?,
    val reasonValue1: Int?,
    val reasonValue2: Int?,
    val weatherCondition: String?,
    val precipitationProbability: Int?,
    val precipitationAmountMm: Double?,
    val source: String?,
    val alarmId: Long? = null,
)
