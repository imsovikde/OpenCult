package org.opencult.scale.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.opencult.scale.core.database.dao.HealthConnectLedgerDao
import org.opencult.scale.core.database.dao.HouseholdProfileDao
import org.opencult.scale.core.database.dao.MeasurementDao
import org.opencult.scale.core.database.dao.RawBlePacketDao
import org.opencult.scale.core.database.dao.ScaleDeviceDao
import org.opencult.scale.core.database.dao.SyncOutboxDao
import org.opencult.scale.core.database.entity.HealthConnectSyncLedgerEntity
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.database.entity.RawBlePacketEntity
import org.opencult.scale.core.database.entity.ScaleDeviceEntity
import org.opencult.scale.core.database.entity.SyncOutboxEntity

@Database(
    entities = [
        HouseholdProfileEntity::class,
        ScaleDeviceEntity::class,
        MeasurementEntity::class,
        RawBlePacketEntity::class,
        HealthConnectSyncLedgerEntity::class,
        SyncOutboxEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OpenCultDatabase : RoomDatabase() {
    abstract fun householdProfileDao(): HouseholdProfileDao
    abstract fun scaleDeviceDao(): ScaleDeviceDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun rawBlePacketDao(): RawBlePacketDao
    abstract fun healthConnectLedgerDao(): HealthConnectLedgerDao
    abstract fun syncOutboxDao(): SyncOutboxDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOpenCultDatabase(@ApplicationContext context: Context): OpenCultDatabase =
        Room.databaseBuilder(context, OpenCultDatabase::class.java, "opencult.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides fun provideHouseholdProfileDao(db: OpenCultDatabase): HouseholdProfileDao = db.householdProfileDao()
    @Provides fun provideScaleDeviceDao(db: OpenCultDatabase): ScaleDeviceDao = db.scaleDeviceDao()
    @Provides fun provideMeasurementDao(db: OpenCultDatabase): MeasurementDao = db.measurementDao()
    @Provides fun provideRawBlePacketDao(db: OpenCultDatabase): RawBlePacketDao = db.rawBlePacketDao()
    @Provides fun provideHealthConnectLedgerDao(db: OpenCultDatabase): HealthConnectLedgerDao = db.healthConnectLedgerDao()
    @Provides fun provideSyncOutboxDao(db: OpenCultDatabase): SyncOutboxDao = db.syncOutboxDao()
}
