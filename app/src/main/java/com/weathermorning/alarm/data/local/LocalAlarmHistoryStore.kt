package com.weathermorning.alarm.data.local

import android.content.Context
import com.weathermorning.alarm.data.local.db.AlarmHistoryEntity
import com.weathermorning.alarm.data.local.db.AppDatabase
import com.weathermorning.alarm.domain.model.AlarmDecision
import com.weathermorning.alarm.domain.model.AlarmHistoryEntry
import com.weathermorning.alarm.domain.model.AlarmHistoryType
import com.weathermorning.alarm.domain.model.AlarmReasonCode
import com.weathermorning.alarm.domain.model.WeatherCondition
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalAlarmHistoryStore(context: Context) {
    private val dao = AppDatabase.get(context).alarmHistoryDao()

    suspend fun append(
        type: AlarmHistoryType,
        reason: String,
        decision: AlarmDecision? = null,
        reasonCode: AlarmReasonCode? = null,
        reasonCondition: WeatherCondition? = null,
        reasonValue1: Int? = null,
        reasonValue2: Int? = null,
        alarmId: Long? = null,
    ) {
        val now = Instant.now()
        val entry = AlarmHistoryEntity(
            // A millisecond is not unique enough: the precheck decision and its
            // provider diagnostic are written back to back, and an id collision
            // silently replaced one of them.
            id = UUID.randomUUID().toString(),
            occurredAtEpochMillis = now.toEpochMilli(),
            type = type.name,
            reason = reason,
            reasonCode = (decision?.reasonCode ?: reasonCode)?.name,
            reasonCondition = (decision?.reasonCondition ?: reasonCondition)?.name,
            reasonValue1 = decision?.reasonValue1 ?: reasonValue1,
            reasonValue2 = decision?.reasonValue2 ?: reasonValue2,
            weatherCondition = decision?.weatherSnapshot?.condition?.name,
            precipitationProbability = decision?.weatherSnapshot?.precipitationProbability,
            precipitationAmountMm = decision?.weatherSnapshot?.precipitationAmountMm,
            source = decision?.weatherSnapshot?.source,
            alarmId = alarmId,
        )
        withContext(Dispatchers.IO) {
            dao.insert(entry)
            dao.trimTo(MAX_ENTRIES)
        }
    }

    fun observeLatest(limit: Int = MAX_ENTRIES): Flow<List<AlarmHistoryEntry>> {
        return dao.observeLatest(limit).map { entries ->
            entries.map { it.toDomain() }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            dao.clear()
        }
    }

    private fun AlarmHistoryEntity.toDomain(): AlarmHistoryEntry {
        return AlarmHistoryEntry(
            id = id,
            occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
            type = AlarmHistoryType.valueOf(type),
            reason = reason,
            reasonCode = reasonCode?.let { AlarmReasonCode.valueOf(it) },
            reasonCondition = reasonCondition?.let { WeatherCondition.valueOf(it) },
            reasonValue1 = reasonValue1,
            reasonValue2 = reasonValue2,
            weatherCondition = weatherCondition?.let { WeatherCondition.valueOf(it) },
            precipitationProbability = precipitationProbability,
            precipitationAmountMm = precipitationAmountMm,
            source = source,
            alarmId = alarmId,
        )
    }

    companion object {
        /**
         * "Why did it not ring?" is the question this app exists to answer, and
         * prechecks and diagnostics share the same budget, so five entries did
         * not survive a single day.
         */
        const val MAX_ENTRIES = 60
    }
}
