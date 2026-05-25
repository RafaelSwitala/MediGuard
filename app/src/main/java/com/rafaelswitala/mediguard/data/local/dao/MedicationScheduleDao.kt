package com.rafaelswitala.mediguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rafaelswitala.mediguard.data.local.entity.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for MedicationSchedule entities
 * Provides database operations for medication schedules
 */
@Dao
interface MedicationScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: MedicationScheduleEntity): Long

    @Update
    suspend fun update(schedule: MedicationScheduleEntity)

    @Delete
    suspend fun delete(schedule: MedicationScheduleEntity)

    @Query("SELECT * FROM medication_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): MedicationScheduleEntity?

    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId")
    fun getSchedulesByMedicationId(medicationId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE medicationId = :medicationId AND isEnabled = 1")
    fun getActiveSchedulesByMedicationId(medicationId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE isEnabled = 1")
    fun getAllActiveSchedules(): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules")
    fun getAllSchedules(): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE scheduleType = :scheduleType")
    fun getSchedulesByType(scheduleType: String): Flow<List<MedicationScheduleEntity>>

    @Query("UPDATE medication_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateScheduleStatus(id: Long, enabled: Boolean)

    @Query("DELETE FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesByMedicationId(medicationId: Long)
}
