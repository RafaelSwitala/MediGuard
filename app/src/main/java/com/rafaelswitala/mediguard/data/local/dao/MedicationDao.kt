package com.rafaelswitala.mediguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rafaelswitala.mediguard.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO für Medikamente.
 */
@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE isActive = 1")
    fun getAllActiveMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun getMedicationsOrderedByDate(): Flow<List<MedicationEntity>>

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE medications SET isActive = 0 WHERE id = :id")
    suspend fun deactivateMedication(id: Long)

    @Query("UPDATE medications SET remainingDoses = :remainingDoses WHERE id = :id")
    suspend fun updateRemainingDoses(id: Long, remainingDoses: Int?)

    @Query("UPDATE medications SET remainingDosesDecimal = :remainingDoses WHERE id = :id")
    suspend fun updateRemainingDosesDecimal(id: Long, remainingDoses: Double?)

    @Query("UPDATE medications SET groupName = :groupName WHERE id = :id")
    suspend fun updateMedicationGroup(id: Long, groupName: String?)

    @Query("UPDATE medications SET intakeGroupId = :groupId WHERE id = :id")
    suspend fun updateIntakeGroup(id: Long, groupId: Int?)

    @Query("UPDATE medications SET remainingVolumeMl = :volume WHERE id = :id")
    suspend fun updateRemainingVolume(id: Long, volume: Double?)
}
