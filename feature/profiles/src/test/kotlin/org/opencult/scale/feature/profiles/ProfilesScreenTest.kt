package org.opencult.scale.feature.profiles

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class ProfilesScreenTest {

    @Test
    fun lossless_export_bundle_round_trips_raw_packets_and_metrics_without_loss() {
        val bundle = LosslessExportBundle(
            exportedAtIso = "2026-11-26T10:00:00Z",
            profiles = listOf(
                ExportProfileRow(
                    id = "prof-1",
                    displayName = "Souvik",
                    biologicalSex = "male",
                    birthDate = "1995-05-14",
                    heightCm = 176.0,
                    activityLevel = "moderate",
                    autoMatchWindowKg = 2.5,
                ),
            ),
            measurements = listOf(
                ExportMeasurementRow(
                    id = "meas-1",
                    profileId = "prof-1",
                    measuredAtMs = 1_764_150_000_000L,
                    source = "ble_8e_handles",
                    assignmentState = "confirmed",
                    weightKg = 74.8,
                    heartRateBpm = 68,
                    metricsJson = """{"weight_kg":74.8}""",
                    provenanceJson = """{"weight_kg":"DIRECT_HARDWARE"}""",
                    rawBlePacketsHex = listOf("0xBE:BE00...", "0xDF:DF00..."),
                ),
            ),
        )

        val encoded = Json.encodeToString(bundle)
        val decoded = Json.decodeFromString<LosslessExportBundle>(encoded)
        assertThat(decoded).isEqualTo(bundle)
        assertThat(decoded.measurements.single().rawBlePacketsHex).hasSize(2)
    }

    @Test
    fun profiles_ui_state_defaults_to_unlocked_member_and_audio_enabled() {
        val state = ProfilesUiState()
        assertThat(state.memberLockEnabled).isFalse()
        assertThat(state.soundFeedbackEnabled).isTrue()
        assertThat(state.profiles).isEmpty()
    }
}
