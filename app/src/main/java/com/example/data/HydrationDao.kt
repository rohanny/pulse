package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.UserSettings
import com.example.model.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsOnce(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettings)

    @Query("SELECT * FROM water_logs ORDER BY loggedAtEpochMs DESC")
    fun observeAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE dateKey = :dateKey")
    suspend fun getTotalMlForDate(dateKey: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog): Long

    @Query("DELETE FROM water_logs WHERE id = :logId")
    suspend fun deleteWaterLogById(logId: Long)
}
