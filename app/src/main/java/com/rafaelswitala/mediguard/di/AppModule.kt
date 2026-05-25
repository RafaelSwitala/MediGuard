package com.rafaelswitala.mediguard.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.alarm.AndroidAlarmScheduler
import com.rafaelswitala.mediguard.data.local.AppDatabase
import com.rafaelswitala.mediguard.data.local.dao.IntakeHistoryDao
import com.rafaelswitala.mediguard.data.local.dao.MedicationDao
import com.rafaelswitala.mediguard.data.local.dao.MedicationScheduleDao
import com.rafaelswitala.mediguard.data.repository.IntakeHistoryRepositoryImpl
import com.rafaelswitala.mediguard.data.repository.MedicationRepositoryImpl
import com.rafaelswitala.mediguard.data.repository.MedicationScheduleRepositoryImpl
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module
 * Provides singletons for database, DAOs, repositories, and other services
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE medications ADD COLUMN durationDays INTEGER")
            database.execSQL("ALTER TABLE medications ADD COLUMN remainingDoses INTEGER")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE medications ADD COLUMN treatmentType TEXT NOT NULL DEFAULT 'ONGOING'")
            database.execSQL("ALTER TABLE medications ADD COLUMN supplyAlertEnabled INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE medications ADD COLUMN supplyAlertThreshold INTEGER NOT NULL DEFAULT 7")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE medications ADD COLUMN groupName TEXT")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE medications ADD COLUMN doseQuantity INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE medications ADD COLUMN intakeGroupId INTEGER")
            database.execSQL("ALTER TABLE medications ADD COLUMN medicationFormType TEXT NOT NULL DEFAULT 'TABLET'")
            database.execSQL("ALTER TABLE medications ADD COLUMN treatmentLimitDoses INTEGER")
            database.execSQL("ALTER TABLE medications ADD COLUMN remainingVolumeMl REAL")
            database.execSQL("ALTER TABLE medications ADD COLUMN dosePerIntakeMl REAL")
        }
    }

    // Database
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()

    // DAOs
    @Provides
    @Singleton
    fun provideMedicationDao(database: AppDatabase): MedicationDao =
        database.medicationDao()

    @Provides
    @Singleton
    fun provideMedicationScheduleDao(database: AppDatabase): MedicationScheduleDao =
        database.medicationScheduleDao()

    @Provides
    @Singleton
    fun provideIntakeHistoryDao(database: AppDatabase): IntakeHistoryDao =
        database.intakeHistoryDao()

    // Repositories
    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: MedicationDao
    ): MedicationRepository = MedicationRepositoryImpl(medicationDao)

    @Provides
    @Singleton
    fun provideMedicationScheduleRepository(
        scheduleDao: MedicationScheduleDao
    ): MedicationScheduleRepository = MedicationScheduleRepositoryImpl(scheduleDao)

    @Provides
    @Singleton
    fun provideIntakeHistoryRepository(
        intakeHistoryDao: IntakeHistoryDao
    ): IntakeHistoryRepository = IntakeHistoryRepositoryImpl(intakeHistoryDao)

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        androidAlarmScheduler: AndroidAlarmScheduler
    ): AlarmScheduler = androidAlarmScheduler
}
