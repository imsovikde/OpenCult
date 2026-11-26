package org.opencult.scale.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.opencult.scale.core.database.entity.HealthConnectSyncLedgerEntity
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.database.entity.RawBlePacketEntity
import org.opencult.scale.core.database.entity.ScaleDeviceEntity
import org.opencult.scale.core.database.entity.SyncOutboxEntity

@Dao
interface HouseholdProfileDao {
    @Query("SELECT * FROM household_profiles WHERE deleted_at_ms IS NULL ORDER BY sort_order ASC, created_at_ms ASC")
    fun observeActiveProfiles(): Flow<List<HouseholdProfileEntity>>

    @Query("SELECT * FROM household_profiles WHERE deleted_at_ms IS NULL ORDER BY sort_order ASC, created_at_ms ASC")
    suspend fun getActiveProfiles(): List<HouseholdProfileEntity>

    @Query("SELECT COUNT(*) FROM household_profiles WHERE deleted_at_ms IS NULL")
    suspend fun countActiveProfiles(): Int

    @Query("SELECT * FROM household_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HouseholdProfileEntity?

    @Upsert
    suspend fun upsert(profile: HouseholdProfileEntity)

    @Query("UPDATE household_profiles SET deleted_at_ms = :deletedAtMs, updated_at_ms = :deletedAtMs WHERE id = :id")
    suspend fun softDelete(id: String, deletedAtMs: Long)
}

@Dao
interface ScaleDeviceDao {
    @Query("SELECT * FROM scale_devices WHERE deleted_at_ms IS NULL ORDER BY last_seen_at_ms DESC, created_at_ms ASC")
    fun observeDevices(): Flow<List<ScaleDeviceEntity>>

    @Query("SELECT * FROM scale_devices WHERE deleted_at_ms IS NULL ORDER BY last_seen_at_ms DESC")
    suspend fun getDevices(): List<ScaleDeviceEntity>

    @Query("SELECT * FROM scale_devices WHERE account_id = :accountId AND ble_mac_or_id = :mac LIMIT 1")
    suspend fun findByMac(accountId: String, mac: String): ScaleDeviceEntity?

    @Upsert
    suspend fun upsert(device: ScaleDeviceEntity)
}

@Dao
interface MeasurementDao {
    @Query(
        "SELECT * FROM measurements " +
            "WHERE profile_id = :profileId AND deleted_at_ms IS NULL " +
            "ORDER BY measured_at_ms DESC",
    )
    fun observeByProfile(profileId: String): Flow<List<MeasurementEntity>>

    @Query(
        "SELECT * FROM measurements " +
            "WHERE profile_id = :profileId AND deleted_at_ms IS NULL " +
            "ORDER BY measured_at_ms DESC LIMIT :limit",
    )
    suspend fun getRecentForProfile(profileId: String, limit: Int = 30): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE deleted_at_ms IS NULL ORDER BY measured_at_ms DESC")
    fun observeAllActive(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MeasurementEntity?

    @Query("SELECT DISTINCT local_date FROM measurements WHERE profile_id = :profileId AND deleted_at_ms IS NULL")
    fun observeActiveDatesForProfile(profileId: String): Flow<List<String>>

    @Query(
        "SELECT * FROM measurements WHERE deleted_at_ms IS NULL AND profile_id = :profileId " +
            "ORDER BY measured_at_ms DESC LIMIT 1",
    )
    suspend fun getLatestForProfile(profileId: String): MeasurementEntity?

    @Upsert
    suspend fun upsert(measurement: MeasurementEntity)

    @Query(
        "UPDATE measurements SET profile_id = :profileId, assignment_state = 'confirmed', " +
            "metrics_json = :metricsJson, provenance_json = :provenanceJson, " +
            "mutation_id = :mutationId, updated_at_ms = :updatedAtMs WHERE id = :id",
    )
    suspend fun updateReassignedProfileAndMetrics(
        id: String,
        profileId: String,
        metricsJson: String,
        provenanceJson: String,
        mutationId: String,
        updatedAtMs: Long,
    )

    @Query(
        "UPDATE measurements SET deleted_at_ms = :deletedAtMs, updated_at_ms = :deletedAtMs, mutation_id = :mutationId " +
            "WHERE id = :id",
    )
    suspend fun softDelete(id: String, mutationId: String, deletedAtMs: Long)

    @Query("UPDATE measurements SET account_id = :newAccountId WHERE account_id = 'local-guest'")
    suspend fun claimGuestMeasurements(newAccountId: String)

    @Query("UPDATE household_profiles SET account_id = :newAccountId WHERE account_id = 'local-guest'")
    suspend fun claimGuestProfiles(newAccountId: String)

    @Query("UPDATE scale_devices SET account_id = :newAccountId WHERE account_id = 'local-guest'")
    suspend fun claimGuestDevices(newAccountId: String)
}

@Dao
interface RawBlePacketDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(packets: List<RawBlePacketEntity>)

    @Query("SELECT * FROM raw_ble_packets WHERE measurement_id = :measurementId ORDER BY seq_no ASC")
    suspend fun getForMeasurement(measurementId: String): List<RawBlePacketEntity>

    @Query("SELECT * FROM raw_ble_packets ORDER BY captured_at_ms DESC LIMIT :limit")
    fun observeLatest(limit: Int = 40): Flow<List<RawBlePacketEntity>>
}

@Dao
interface HealthConnectLedgerDao {
    @Query("SELECT * FROM health_connect_sync_ledger WHERE measurement_id = :measurementId")
    suspend fun getForMeasurement(measurementId: String): List<HealthConnectSyncLedgerEntity>

    @Query("SELECT * FROM health_connect_sync_ledger ORDER BY updated_at_ms DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<HealthConnectSyncLedgerEntity>>

    @Query("SELECT * FROM health_connect_sync_ledger WHERE sync_status IN ('pending', 'failed')")
    suspend fun getPendingOrFailed(): List<HealthConnectSyncLedgerEntity>

    @Query("SELECT * FROM health_connect_sync_ledger WHERE client_record_id = :clientRecordId LIMIT 1")
    suspend fun getByClientRecordId(clientRecordId: String): HealthConnectSyncLedgerEntity?

    @Upsert
    suspend fun upsertAll(entries: List<HealthConnectSyncLedgerEntity>)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_outbox ORDER BY created_at_ms ASC LIMIT :limit")
    suspend fun getReadyBatch(limit: Int = 50): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAll(items: List<SyncOutboxEntity>)

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query(
        "UPDATE sync_outbox SET attempt_count = attempt_count + 1, last_error = :error, " +
            "next_attempt_at_ms = :nextAttemptMs WHERE id = :id",
    )
    suspend fun markFailed(id: String, error: String, nextAttemptMs: Long)
}
