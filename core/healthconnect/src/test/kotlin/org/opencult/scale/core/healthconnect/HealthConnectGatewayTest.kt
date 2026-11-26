package org.opencult.scale.core.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
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
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectGatewayTest {

    private lateinit var db: OpenCultDatabase
    private lateinit var repo: OpenCultRepository

    private class FakePlatformClient(
        var granted: Set<String> = HealthConnectGateway.ALL_PERMISSIONS,
        var unsupportedFeatures: Set<Int> = emptySet(),
    ) : HealthConnectPlatformClient {
        val inserted = mutableListOf<Record>()
        val deletedClientIds = mutableListOf<String>()
        var nextChanges = ExternalChangesBatch("tok-2", emptyList(), emptyList())

        override fun getAvailability() = HealthConnectAvailability.AVAILABLE
        override fun isFeatureAvailable(feature: Int): Boolean = feature !in unsupportedFeatures
        override suspend fun getGrantedPermissions() = granted
        override suspend fun insertRecords(records: List<Record>): List<String> {
            inserted += records
            return records.indices.map { "uid-$it" }
        }
        override suspend fun deleteRecordsByClientId(recordType: KClass<out Record>, clientIds: List<String>) {
            deletedClientIds += clientIds
        }
        override suspend fun readDailyContext(date: LocalDate, includeRestingHr: Boolean) =
            DailyHealthContext(date, 8450L, 450L, 2180, activeCaloriesKcal = 620, restingHeartRateBpm = if (includeRestingHr) 58 else null)
        override suspend fun getWeightChangesToken() = "tok-1"
        override suspend fun fetchExternalWeightChanges(token: String, selfPackageName: String) = nextChanges
    }

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
    fun primary_member_writes_all_granted_records_and_increments_client_version_idempotently() = runTest {
        val fake = FakePlatformClient(
            granted = setOf(
                HealthPermission.getWritePermission(WeightRecord::class),
                HealthPermission.getWritePermission(BodyFatRecord::class),
            ),
        )
        val gw = HealthConnectGateway(ApplicationProvider.getApplicationContext(), fake, db, repo)
        val now = 1_760_000_000_000L
        val profile = HouseholdProfileEntity(
            id = "p1", accountId = "acc", displayName = "Souvik", avatarColor = "#2185F7",
            biologicalSex = "male", birthDate = "1994-01-01", heightCm = 178.0,
            activityLevel = "moderate", athleteMode = false, isPrimaryHcUser = true,
            createdAtMs = now, updatedAtMs = now,
        )
        repo.createOrUpdateProfile(profile)
        val report = BodyMetricsEngine.calculate(
            UserAnthropometry(BiologicalSex.MALE, 31, 178.0),
            ScaleTier.CULT_PRO_8E, 74.8, 68,
            DualFreqImpedance(307.4, 311.3, 26.8, 242.1, 256.2, 267.6, 275.5, 24.5, 219.1, 229.5),
        )
        val m = repo.commitMeasurementAtomic("acc", profile.id, "confirmed", null, report, now, 540.0, 520.35, emptyList())

        val first = gw.syncMeasurement(m, profile)
        assertThat(first.writtenRecords).isEqualTo(2)
        assertThat(fake.inserted.first().metadata.clientRecordVersion).isEqualTo(1L)

        val second = gw.syncMeasurement(m, profile)
        assertThat(second.writtenRecords).isEqualTo(2)
        assertThat(fake.inserted.last().metadata.clientRecordVersion).isEqualTo(2L)

        gw.deleteSyncedRecordsForMeasurement(m.id)
        assertThat(fake.deletedClientIds).containsExactly(
            HealthConnectGateway.clientRecordId(m.id, "weight"),
            HealthConnectGateway.clientRecordId(m.id, "body_fat"),
        )
    }

    @Test
    fun reassigning_primary_to_non_primary_deletes_old_hc_records_and_skips_new_writes_inv05() = runTest {
        val fake = FakePlatformClient(
            granted = setOf(
                HealthPermission.getWritePermission(WeightRecord::class),
                HealthPermission.getWritePermission(BodyFatRecord::class),
            ),
        )
        val gw = HealthConnectGateway(ApplicationProvider.getApplicationContext(), fake, db, repo)
        val now = 1_760_000_000_000L
        val primary = HouseholdProfileEntity(
            id = "p1", accountId = "acc", displayName = "Souvik", avatarColor = "#2185F7",
            biologicalSex = "male", birthDate = "1994-01-01", heightCm = 178.0,
            activityLevel = "moderate", athleteMode = false, isPrimaryHcUser = true,
            createdAtMs = now, updatedAtMs = now,
        )
        val nonPrimary = HouseholdProfileEntity(
            id = "p2", accountId = "acc", displayName = "Guest", avatarColor = "#10B981",
            biologicalSex = "female", birthDate = "1996-01-01", heightCm = 165.0,
            activityLevel = "mild", athleteMode = false, isPrimaryHcUser = false,
            createdAtMs = now, updatedAtMs = now,
        )
        repo.createOrUpdateProfile(primary)
        repo.createOrUpdateProfile(nonPrimary)

        val report = BodyMetricsEngine.calculate(
            UserAnthropometry(BiologicalSex.MALE, 31, 178.0),
            ScaleTier.CULT_BF01_4E, 68.4, 66, singleFreq50KhzOhm = 500.0,
        )
        val m = repo.commitMeasurementAtomic("acc", primary.id, "confirmed", null, report, now, 500.0, null, emptyList())
        gw.syncMeasurement(m, primary)
        val initialInsertCount = fake.inserted.size

        // Reassign Primary -> Non-Primary: old HC records MUST be deleted, ZERO new HC writes for Guest
        val updated = gw.reassignMeasurementAndSyncHealthConnect(m.id, nonPrimary.id)!!
        assertThat(updated.profileId).isEqualTo(nonPrimary.id)
        assertThat(fake.deletedClientIds).contains(HealthConnectGateway.clientRecordId(m.id, "weight"))
        assertThat(fake.inserted).hasSize(initialInsertCount)
    }

    @Test
    fun polls_and_imports_external_hc_weights_then_soft_deletes_on_deletion_change() = runTest {
        val fake = FakePlatformClient()
        val gw = HealthConnectGateway(ApplicationProvider.getApplicationContext(), fake, db, repo)
        val now = 1_760_000_000_000L
        val primary = repo.ensureDefaultProfileIfEmpty("acc")

        fake.nextChanges = ExternalChangesBatch(
            nextToken = "tok-3",
            importedWeights = listOf(ExternalWeightImport("ext-hc-rec-99", 73.25, now)),
            deletedExternalIds = emptyList(),
        )
        val apply1 = gw.pollAndImportExternalWeights(null, "acc")!!
        assertThat(apply1.importedCount).isEqualTo(1)
        val detId = OpenCultRepository.externalImportUuid("ext-hc-rec-99")
        val saved = db.measurementDao().getById(detId)!!
        assertThat(saved.source).isEqualTo("health_connect_import")
        assertThat(saved.profileId).isEqualTo(primary.id)
        assertThat(saved.weightKg).isWithin(0.01).of(73.25)

        fake.nextChanges = ExternalChangesBatch(
            nextToken = "tok-4",
            importedWeights = emptyList(),
            deletedExternalIds = listOf("ext-hc-rec-99"),
        )
        val apply2 = gw.pollAndImportExternalWeights("tok-3", "acc")!!
        assertThat(apply2.deletedCount).isEqualTo(1)
        assertThat(db.measurementDao().getById(detId)!!.deletedAtMs).isNotNull()
    }

    @Test
    fun gates_history_and_background_permissions_by_health_connect_feature_availability() {
        val limitedClient = FakePlatformClient(
            unsupportedFeatures = setOf(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY,
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
            ),
        )
        val gw = HealthConnectGateway(ApplicationProvider.getApplicationContext(), limitedClient, db, repo)
        val perms = gw.supportedRequestedPermissions(includeRestingHr = true, includeBackgroundRead = true)

        assertThat(perms).contains(HealthConnectGateway.OPT_IN_RESTING_HR_PERMISSION)
        assertThat(perms).doesNotContain(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
        assertThat(perms).doesNotContain(HealthConnectGateway.OPT_IN_BACKGROUND_READ_PERMISSION)
    }
}
