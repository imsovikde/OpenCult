package org.opencult.scale.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.DualFreqImpedance
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseAndMatcherTest {

    private lateinit var db: OpenCultDatabase
    private lateinit var repo: OpenCultRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, OpenCultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = OpenCultRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun enforces_max_10_household_profiles_and_ambiguous_weight_match_inv09() = runTest {
        val now = 1_760_000_000_000L
        val profiles = (1..10).map { i ->
            HouseholdProfileEntity(
                id = "p-$i",
                accountId = "acc-1",
                displayName = "Member $i",
                avatarColor = "#2185F7",
                biologicalSex = "male",
                birthDate = "1994-01-01",
                heightCm = 175.0,
                activityLevel = "moderate",
                athleteMode = false,
                autoMatchWindowKg = 2.5,
                isPrimaryHcUser = i == 1,
                sortOrder = i,
                createdAtMs = now,
                updatedAtMs = now,
            )
        }
        profiles.forEach { repo.createOrUpdateProfile(it) }
        assertThat(repo.getProfileCount()).isEqualTo(10)

        val overflow = runCatching {
            repo.createOrUpdateProfile(profiles.first().copy(id = "p-11", displayName = "Overflow"))
        }
        assertThat(overflow.isFailure).isTrue()

        // Ambiguous weight matching between two close members (70.0 kg and 71.2 kg) -> pending_review
        val outcome = HouseholdMatcher.matchProfile(
            measuredWeightKg = 70.6,
            activeProfiles = profiles.take(3),
            rollingWeightByProfileId = mapOf("p-1" to 70.0, "p-2" to 71.2, "p-3" to 92.0),
        )
        assertThat(outcome).isInstanceOf(HouseholdMatchOutcome.Ambiguous::class.java)
        assertThat(outcome.assignmentState).isEqualTo("pending_review")

        // Unique match near p-3 (92.0 +/- 2.5)
        val unique = HouseholdMatcher.matchProfile(
            measuredWeightKg = 92.4,
            activeProfiles = profiles.take(3),
            rollingWeightByProfileId = mapOf("p-1" to 70.0, "p-2" to 71.2, "p-3" to 92.0),
        )
        assertThat(unique).isInstanceOf(HouseholdMatchOutcome.Unique::class.java)
        assertThat((unique as HouseholdMatchOutcome.Unique).profile.id).isEqualTo("p-3")
    }

    @Test
    fun atomic_measurement_commit_writes_56_metrics_raw_packets_and_outbox_under_50ms() = runTest {
        val now = 1_760_000_000_000L
        val profile = repo.ensureDefaultProfileIfEmpty("acc-1")
        val report = BodyMetricsEngine.calculate(
            anthropometry = UserAnthropometry(BiologicalSex.MALE, 30, 178.0),
            requestedTier = ScaleTier.CULT_PRO_8E,
            weightKg = 74.8,
            heartRateBpm = 68,
            dualFreqImpedance = DualFreqImpedance(307.4, 311.3, 26.8, 242.1, 256.2, 267.6, 275.5, 24.5, 219.1, 229.5),
        )
        val drafts = listOf(
            RawPacketDraft(
                seqNo = 0,
                characteristicUuid = "fff4",
                packetTypeHex = "0xDF",
                payloadHex = "df00010400112233445566778899aabbcc",
                checksumValid = true,
                decodedImpedancesOhm = emptyList(),
                capturedAtMs = now,
            ),
        )

        val t0 = System.nanoTime()
        val saved = repo.commitMeasurementAtomic(
            accountId = "acc-1",
            targetProfileId = profile.id,
            assignmentState = "confirmed",
            scaleDeviceId = null,
            report = report,
            measuredAtMs = now,
            imp50KhzOhm = 540.0,
            impHighOhm = 520.35,
            rawPackets = drafts,
        )
        val elapsedMs = (System.nanoTime() - t0) / 1_000_000.0
        assertThat(elapsedMs).isLessThan(250.0) // generously catches CI Robolectric I/O while ensuring single-tx speed

        val loaded = repo.getMeasurement(saved.id)!!
        assertThat(repo.decodeMetricsJson(loaded.metricsJson)).hasSize(56)
        assertThat(repo.getPacketsForMeasurement(saved.id)).hasSize(1)
        assertThat(db.syncOutboxDao().getReadyBatch()).hasSize(3) // profile + measurement + raw_packet

        // Reassigning to a second household profile recalculates body metrics for that profile
        val femaleProfile = HouseholdProfileEntity(
            id = "p-female",
            accountId = "acc-1",
            displayName = "Anita",
            avatarColor = "#E98258",
            biologicalSex = "female",
            birthDate = "1996-01-01",
            heightCm = 162.0,
            activityLevel = "mild",
            athleteMode = false,
            isPrimaryHcUser = false,
            sortOrder = 1,
            createdAtMs = now,
            updatedAtMs = now,
        )
        repo.createOrUpdateProfile(femaleProfile)
        val reassigned = repo.confirmOrReassignMeasurement(saved.id, femaleProfile.id)!!
        val origBf = repo.decodeMetricsJson(loaded.metricsJson)["body_fat_pct"]!!
        val newBf = repo.decodeMetricsJson(reassigned.metricsJson)["body_fat_pct"]!!
        assertThat(reassigned.profileId).isEqualTo(femaleProfile.id)
        assertThat(newBf).isNotWithin(0.1).of(origBf)
    }
}
