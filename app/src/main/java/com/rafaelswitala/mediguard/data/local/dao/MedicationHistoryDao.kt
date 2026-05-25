package com.rafaelswitala.mediguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rafaelswitala.mediguard.data.local.entity.IntakeHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for IntakeHistory entities
 * Provides database operations for medication intake records
 */
@Dao
interface IntakeHistoryDao {
    @Insert
    suspend fun insert(intakeHistory: IntakeHistoryEntity): Long

    @Update
    suspend fun update(intakeHistory: IntakeHistoryEntity)

    @Delete
    suspend fun delete(intakeHistory: IntakeHistoryEntity)

    @Query("SELECT * FROM intake_history WHERE id = :id")
    suspend fun getIntakeHistoryById(id: Long): IntakeHistoryEntity?

    @Query("SELECT * FROM intake_history WHERE medicationId = :medicationId")
    fun getIntakeHistoryByMedicationId(medicationId: Long): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history ORDER BY scheduledTime DESC")
    fun getAllIntakeHistory(): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history WHERE scheduledTime >= :from AND scheduledTime <= :to")
    fun getIntakeHistoryInRange(from: Long, to: Long): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history WHERE status = :status")
    fun getIntakeHistoryByStatus(status: String): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history WHERE medicationId = :medicationId AND status = :status")
    fun getIntakeHistoryByMedicationAndStatus(
        medicationId: Long,
        status: String
    ): Flow<List<IntakeHistoryEntity>>

    @Query("UPDATE intake_history SET status = :status, actualIntakeTime = :intakeTime WHERE id = :id")
    suspend fun updateIntakeStatus(id: Long, status: String, intakeTime: Long?)
}