package org.opencult.scale.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "household_profiles",
    indices = [
        Index(value = ["account_id", "sort_order"]),
    ],
)
data class HouseholdProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "avatar_color") val avatarColor: String,
    @ColumnInfo(name = "biological_sex") val biologicalSex: String,
    @ColumnInfo(name = "birth_date") val birthDate: String,
    @ColumnInfo(name = "height_cm") val heightCm: Double,
    @ColumnInfo(name = "activity_level") val activityLevel: String,
    @ColumnInfo(name = "athlete_mode") val athleteMode: Boolean,
    @ColumnInfo(name = "auto_match_window_kg") val autoMatchWindowKg: Double = 2.50,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double? = null,
    @ColumnInfo(name = "target_body_fat_pct") val targetBodyFatPct: Double? = null,
    @ColumnInfo(name = "is_primary_hc_user") val isPrimaryHcUser: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
    @ColumnInfo(name = "deleted_at_ms") val deletedAtMs: Long? = null,
)

@Entity(
    tableName = "scale_devices",
    indices = [
        Index(value = ["account_id", "ble_mac_or_id"], unique = true),
    ],
)
data class ScaleDeviceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "ble_mac_or_id") val bleMacOrId: String,
    @ColumnInfo(name = "advertised_name") val advertisedName: String,
    @ColumnInfo(name = "model_tier") val modelTier: String,
    @ColumnInfo(name = "electrode_count") val electrodeCount: Int,
    @ColumnInfo(name = "firmware_notes") val firmwareNotes: String? = null,
    @ColumnInfo(name = "last_battery_pct") val lastBatteryPct: Int? = null,
    @ColumnInfo(name = "last_seen_at_ms") val lastSeenAtMs: Long? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
    @ColumnInfo(name = "deleted_at_ms") val deletedAtMs: Long? = null,
)

@Entity(
    tableName = "measurements",
    indices = [
        Index(value = ["profile_id", "local_date", "measured_at_ms"]),
        Index(value = ["account_id", "updated_at_ms"]),
    ],
)
data class MeasurementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "profile_id") val profileId: String,
    @ColumnInfo(name = "scale_device_id") val scaleDeviceId: String?,
    @ColumnInfo(name = "measured_at_ms") val measuredAtMs: Long,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "assignment_state") val assignmentState: String,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "heart_rate_bpm") val heartRateBpm: Int? = null,
    @ColumnInfo(name = "impedance_50khz_ohm") val impedance50KhzOhm: Double? = null,
    @ColumnInfo(name = "impedance_high_ohm") val impedanceHighOhm: Double? = null,
    @ColumnInfo(name = "metrics_json") val metricsJson: String,
    @ColumnInfo(name = "provenance_json") val provenanceJson: String,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "mutation_id") val mutationId: String,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
    @ColumnInfo(name = "deleted_at_ms") val deletedAtMs: Long? = null,
)

/** Append-only raw BLE hex frame ledger (`INV-01`). Never updated after insert. */
@Entity(
    tableName = "raw_ble_packets",
    indices = [
        Index(value = ["measurement_id", "seq_no"]),
    ],
)
data class RawBlePacketEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "measurement_id") val measurementId: String,
    @ColumnInfo(name = "seq_no") val seqNo: Int,
    @ColumnInfo(name = "characteristic_uuid") val characteristicUuid: String,
    @ColumnInfo(name = "packet_type_hex") val packetTypeHex: String,
    @ColumnInfo(name = "payload_hex") val payloadHex: String,
    @ColumnInfo(name = "checksum_valid") val checksumValid: Boolean?,
    @ColumnInfo(name = "decoded_impedances_json") val decodedImpedancesJson: String,
    @ColumnInfo(name = "captured_at_ms") val capturedAtMs: Long,
)

@Entity(
    tableName = "health_connect_sync_ledger",
    indices = [
        Index(value = ["account_id", "client_record_id"], unique = true),
        Index(value = ["measurement_id"]),
    ],
)
data class HealthConnectSyncLedgerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "measurement_id") val measurementId: String,
    @ColumnInfo(name = "hc_record_type") val hcRecordType: String,
    @ColumnInfo(name = "client_record_id") val clientRecordId: String,
    @ColumnInfo(name = "client_record_version") val clientRecordVersion: Long,
    @ColumnInfo(name = "hc_record_uid") val hcRecordUid: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "synced_at_ms") val syncedAtMs: Long? = null,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long,
)

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["next_attempt_at_ms"]),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "operation") val operation: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "mutation_id") val mutationId: String,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long,
    @ColumnInfo(name = "next_attempt_at_ms") val nextAttemptAtMs: Long,
)
