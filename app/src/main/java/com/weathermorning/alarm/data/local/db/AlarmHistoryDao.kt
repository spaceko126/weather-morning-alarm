package com.weathermorning.alarm.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: AlarmHistoryEntity)

    @Query("SELECT * FROM alarm_history ORDER BY occurredAtEpochMillis DESC LIMIT :limit")
    fun loadLatest(limit: Int): List<AlarmHistoryEntity>

    @Query("SELECT * FROM alarm_history ORDER BY occurredAtEpochMillis DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<AlarmHistoryEntity>>

    @Query(
        """
        DELETE FROM alarm_history
        WHERE id NOT IN (
            SELECT id FROM alarm_history ORDER BY occurredAtEpochMillis DESC LIMIT :limit
        )
        """,
    )
    fun trimTo(limit: Int)

    @Query("DELETE FROM alarm_history")
    fun clear()
}
