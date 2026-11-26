package org.opencult.scale.core.sync

import android.content.Intent
import androidx.core.content.IntentSanitizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

data class SupabaseConfig(
    val url: String,
    val anonKey: String,
) {
    val isConfigured: Boolean
        get() = url.startsWith("https://") && anonKey.isNotBlank()
}

data class SyncStatusSnapshot(
    val backendConfigured: Boolean = false,
    val isSignedIn: Boolean = false,
    val accountId: String? = null,
    val userEmail: String? = null,
    val isSyncing: Boolean = false,
    val realtimeConnected: Boolean = false,
    val lastSyncedAtMs: Long? = null,
    val lastError: String? = null,
)

/**
 * Sanitizes incoming OAuth deep-link intents (`android-intent-security` policy)
 * enforcing scheme `opencult`, host `auth`, and path prefix `/callback`.
 */
object OAuthRedirectSanitizer {
    fun sanitizeCallbackIntent(rawIntent: Intent?): Intent? {
        if (rawIntent == null) return null
        val uri = rawIntent.data ?: return null
        val validOrigin = uri.scheme == "opencult" &&
            uri.host == "auth" &&
            (uri.path?.startsWith("/callback") == true)
        if (!validOrigin) return null

        return IntentSanitizer.Builder()
            .allowAnyComponent()
            .allowAction(Intent.ACTION_VIEW)
            .allowCategory(Intent.CATEGORY_DEFAULT)
            .allowCategory(Intent.CATEGORY_BROWSABLE)
            .allowData { candidate ->
                candidate.scheme == "opencult" &&
                    candidate.host == "auth" &&
                    candidate.path?.startsWith("/callback") == true
            }
            .build()
            .sanitizeByFiltering(rawIntent)
    }
}

@Serializable
data class HouseholdProfileDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_color") val avatarColor: String,
    @SerialName("biological_sex") val biologicalSex: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("height_cm") val heightCm: Double,
    @SerialName("activity_level") val activityLevel: String,
    @SerialName("athlete_mode") val athleteMode: Boolean,
    @SerialName("auto_match_window_kg") val autoMatchWindowKg: Double,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = null,
    @SerialName("target_body_fat_pct") val targetBodyFatPct: Double? = null,
    @SerialName("is_primary_hc_user") val isPrimaryHcUser: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class ScaleDeviceDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("ble_mac_or_id") val bleMacOrId: String,
    @SerialName("advertised_name") val advertisedName: String,
    @SerialName("model_tier") val modelTier: String,
    @SerialName("electrode_count") val electrodeCount: Int,
    @SerialName("firmware_notes") val firmwareNotes: String? = null,
    @SerialName("last_battery_pct") val lastBatteryPct: Int? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class MeasurementDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("scale_device_id") val scaleDeviceId: String? = null,
    @SerialName("measured_at") val measuredAt: String,
    val source: String,
    @SerialName("assignment_state") val assignmentState: String,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("heart_rate_bpm") val heartRateBpm: Int? = null,
    @SerialName("impedance_50khz_ohm") val impedance50KhzOhm: Double? = null,
    @SerialName("impedance_high_ohm") val impedanceHighOhm: Double? = null,
    @SerialName("metrics_json") val metricsJson: JsonElement,
    @SerialName("provenance_json") val provenanceJson: JsonElement,
    val notes: String? = null,
    @SerialName("mutation_id") val mutationId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class RawBlePacketDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("measurement_id") val measurementId: String,
    @SerialName("seq_no") val seqNo: Int,
    @SerialName("characteristic_uuid") val characteristicUuid: String,
    @SerialName("packet_type_hex") val packetTypeHex: String,
    @SerialName("payload_hex") val payloadHex: String,
    @SerialName("checksum_valid") val checksumValid: Boolean? = null,
    @SerialName("decoded_impedances_ohm") val decodedImpedancesOhm: List<Double> = emptyList(),
    @SerialName("captured_at") val capturedAt: String,
)
