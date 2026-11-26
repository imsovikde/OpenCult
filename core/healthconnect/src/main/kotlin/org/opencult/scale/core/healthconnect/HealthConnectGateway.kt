package org.opencult.scale.core.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HealthConnectSyncLedgerEntity
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.reflect.KClass

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE,
}

data class DailyHealthContext(
    val date: LocalDate,
    val steps: Long?,
    val sleepMinutes: Long?,
    val totalCaloriesKcal: Int?,
    val activeCaloriesKcal: Int? = null,
    val restingHeartRateBpm: Int? = null,
)

data class HealthConnectWriteSummary(
    val attemptedRecords: Int,
    val writtenRecords: Int,
    val skippedReason: String? = null,
)

data class ExternalWeightImport(
    val recordId: String,
    val weightKg: Double,
    val measuredAtMs: Long,
)

data class ExternalChangesBatch(
    val nextToken: String,
    val importedWeights: List<ExternalWeightImport>,
    val deletedExternalIds: List<String>,
)

data class ExternalSyncApplyResult(
    val nextToken: String,
    val importedCount: Int,
    val deletedCount: Int,
)

interface HealthConnectPlatformClient {
    fun getAvailability(): HealthConnectAvailability
    fun isFeatureAvailable(feature: Int): Boolean = true
    suspend fun getGrantedPermissions(): Set<String>
    suspend fun insertRecords(records: List<Record>): List<String>
    suspend fun deleteRecordsByClientId(recordType: KClass<out Record>, clientIds: List<String>)
    suspend fun readDailyContext(date: LocalDate, includeRestingHr: Boolean = false): DailyHealthContext
    suspend fun getWeightChangesToken(): String
    suspend fun fetchExternalWeightChanges(token: String, selfPackageName: String): ExternalChangesBatch
}

@Singleton
class AndroidHealthConnectPlatformClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthConnectPlatformClient {

    private fun clientOrNull(): HealthConnectClient? =
        if (getAvailability() == HealthConnectAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }

    override fun getAvailability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }

    override fun isFeatureAvailable(feature: Int): Boolean =
        runCatching {
            clientOrNull()?.features?.getFeatureStatus(feature) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        }.getOrDefault(false)

    override suspend fun getGrantedPermissions(): Set<String> =
        clientOrNull()?.permissionController?.getGrantedPermissions() ?: emptySet()

    override suspend fun insertRecords(records: List<Record>): List<String> {
        val client = clientOrNull() ?: return emptyList()
        return client.insertRecords(records).recordIdsList
    }

    override suspend fun deleteRecordsByClientId(recordType: KClass<out Record>, clientIds: List<String>) {
        if (clientIds.isEmpty()) return
        val client = clientOrNull() ?: return
        client.deleteRecords(
            recordType = recordType,
            recordIdsList = emptyList(),
            clientRecordIdsList = clientIds,
        )
    }

    override suspend fun readDailyContext(date: LocalDate, includeRestingHr: Boolean): DailyHealthContext {
        val client = clientOrNull() ?: return DailyHealthContext(date, null, null, null)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    SleepSessionRecord.SLEEP_DURATION_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        val steps = result[StepsRecord.COUNT_TOTAL]
        val sleepMins = result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()
        val totalKcal = result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt()
        val activeKcal = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt()
        val restingHr = if (includeRestingHr) {
            runCatching {
                val rhrResp = client.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                )
                val bpmValues = rhrResp.records.map { it.beatsPerMinute }
                if (bpmValues.isNotEmpty()) bpmValues.average().roundToInt() else null
            }.getOrNull()
        } else {
            null
        }
        return DailyHealthContext(
            date = date,
            steps = steps,
            sleepMinutes = sleepMins,
            totalCaloriesKcal = totalKcal ?: activeKcal,
            activeCaloriesKcal = activeKcal,
            restingHeartRateBpm = restingHr,
        )
    }

    override suspend fun getWeightChangesToken(): String {
        val client = clientOrNull() ?: return ""
        return client.getChangesToken(
            ChangesTokenRequest(recordTypes = setOf(WeightRecord::class)),
        )
    }

    override suspend fun fetchExternalWeightChanges(
        token: String,
        selfPackageName: String,
    ): ExternalChangesBatch {
        val client = clientOrNull() ?: return ExternalChangesBatch(token, emptyList(), emptyList())
        var curToken = token
        val imported = mutableListOf<ExternalWeightImport>()
        val deleted = mutableListOf<String>()

        while (true) {
            val response = client.getChanges(curToken)
            if (response.changesTokenExpired) {
                // Codelab fallback on expired changes token: perform 30-day catch-up read before issuing fresh token
                val now = Instant.now()
                val catchUp = runCatching {
                    client.readRecords(
                        ReadRecordsRequest(
                            recordType = WeightRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(now.minus(30, ChronoUnit.DAYS), now),
                        ),
                    ).records
                }.getOrDefault(emptyList())
                val existingIds = imported.map { it.recordId }.toMutableSet()
                catchUp.forEach { rec ->
                    if (rec.metadata.dataOrigin.packageName != selfPackageName && existingIds.add(rec.metadata.id)) {
                        imported += ExternalWeightImport(
                            recordId = rec.metadata.id,
                            weightKg = rec.weight.inKilograms,
                            measuredAtMs = rec.time.toEpochMilli(),
                        )
                    }
                }
                val refreshed = getWeightChangesToken()
                return ExternalChangesBatch(refreshed, imported, deleted)
            }
            response.changes.forEach { change ->
                when (change) {
                    is UpsertionChange -> {
                        val rec = change.record
                        if (rec is WeightRecord && rec.metadata.dataOrigin.packageName != selfPackageName) {
                            imported += ExternalWeightImport(
                                recordId = rec.metadata.id,
                                weightKg = rec.weight.inKilograms,
                                measuredAtMs = rec.time.toEpochMilli(),
                            )
                        }
                    }
                    is DeletionChange -> deleted += change.recordId
                }
            }
            curToken = response.nextChangesToken
            if (!response.hasMore) break
        }
        return ExternalChangesBatch(curToken, imported, deleted)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthConnectBindingsModule {
    @Binds
    abstract fun bindPlatformClient(impl: AndroidHealthConnectPlatformClient): HealthConnectPlatformClient
}

/**
 * Coordinates idempotent, permission-tolerant writes to Health Connect, orphan cleanup across
 * member reassignment (`INV-05`), historical backfill, and external WeightRecord import (`PRD:HC-01..07`).
 */
@Singleton
class HealthConnectGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val platformClient: HealthConnectPlatformClient,
    private val db: OpenCultDatabase,
    private val repository: OpenCultRepository,
) {
    companion object {
        val WRITE_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getWritePermission(BodyFatRecord::class),
            HealthPermission.getWritePermission(LeanBodyMassRecord::class),
            HealthPermission.getWritePermission(BoneMassRecord::class),
            HealthPermission.getWritePermission(BodyWaterMassRecord::class),
            HealthPermission.getWritePermission(BasalMetabolicRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeightRecord::class),
        )

        val READ_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
        )

        val OPT_IN_RESTING_HR_PERMISSION: String =
            HealthPermission.getReadPermission(RestingHeartRateRecord::class)

        val OPT_IN_BACKGROUND_READ_PERMISSION: String =
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

        val ALL_PERMISSIONS: Set<String> = WRITE_PERMISSIONS + READ_PERMISSIONS

        fun requestedPermissions(
            includeRestingHr: Boolean = false,
            includeBackgroundRead: Boolean = false,
            includeHistoryRead: Boolean = true,
        ): Set<String> = buildSet {
            addAll(WRITE_PERMISSIONS)
            addAll(READ_PERMISSIONS)
            if (!includeHistoryRead) {
                remove(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
            }
            if (includeRestingHr) add(OPT_IN_RESTING_HR_PERMISSION)
            if (includeBackgroundRead) add(OPT_IN_BACKGROUND_READ_PERMISSION)
        }

        fun clientRecordId(measurementId: String, tag: String): String =
            "opencult:$measurementId:$tag"
    }

    fun availability(): HealthConnectAvailability = platformClient.getAvailability()

    fun supportedRequestedPermissions(
        includeRestingHr: Boolean = false,
        includeBackgroundRead: Boolean = false,
    ): Set<String> {
        val historySupported = platformClient.isFeatureAvailable(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY)
        val backgroundSupported = includeBackgroundRead &&
            platformClient.isFeatureAvailable(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND)
        return requestedPermissions(
            includeRestingHr = includeRestingHr,
            includeBackgroundRead = backgroundSupported,
            includeHistoryRead = historySupported,
        )
    }

    suspend fun getGrantedPermissions(): Set<String> = platformClient.getGrantedPermissions()

    suspend fun syncMeasurement(
        measurement: MeasurementEntity,
        profile: HouseholdProfileEntity,
        scaleModelName: String = "Cult Smart Scale",
    ): HealthConnectWriteSummary {
        if (!profile.isPrimaryHcUser) {
            return HealthConnectWriteSummary(0, 0, "Skipped: non-primary household member")
        }
        if (measurement.source == "health_connect_import") {
            return HealthConnectWriteSummary(0, 0, "Skipped: imported Health Connect reading (no echo loop)")
        }
        if (measurement.assignmentState != "confirmed" || measurement.deletedAtMs != null) {
            return HealthConnectWriteSummary(0, 0, "Skipped: measurement pending confirmation or deleted")
        }
        if (platformClient.getAvailability() != HealthConnectAvailability.AVAILABLE) {
            return HealthConnectWriteSummary(0, 0, "Health Connect unavailable")
        }

        val granted = platformClient.getGrantedPermissions()
        val metrics = repository.decodeMetricsJson(measurement.metricsJson)
        val time = Instant.ofEpochMilli(measurement.measuredAtMs)
        val zoneOffset: ZoneOffset = ZoneId.systemDefault().rules.getOffset(time)
        val device = Device(
            type = Device.TYPE_SCALE,
            manufacturer = "Cult.fit",
            model = scaleModelName,
        )

        data class RecordDraft(
            val tag: String,
            val typeName: String,
            val kClass: KClass<out Record>,
            val build: (Metadata) -> Record,
        )

        val drafts = buildList {
            if (HealthPermission.getWritePermission(WeightRecord::class) in granted) {
                add(
                    RecordDraft("weight", "WeightRecord", WeightRecord::class) { meta ->
                        WeightRecord(time, zoneOffset, Mass.kilograms(measurement.weightKg), meta)
                    },
                )
            }
            val bf = metrics["body_fat_pct"]
            if (bf != null && HealthPermission.getWritePermission(BodyFatRecord::class) in granted) {
                add(
                    RecordDraft("body_fat", "BodyFatRecord", BodyFatRecord::class) { meta ->
                        BodyFatRecord(time, zoneOffset, Percentage(bf.coerceIn(1.0, 99.0)), meta)
                    },
                )
            }
            val ffm = metrics["fat_free_mass_kg"]
            if (ffm != null && HealthPermission.getWritePermission(LeanBodyMassRecord::class) in granted) {
                add(
                    RecordDraft("lean_mass", "LeanBodyMassRecord", LeanBodyMassRecord::class) { meta ->
                        LeanBodyMassRecord(time, zoneOffset, Mass.kilograms(ffm), meta)
                    },
                )
            }
            val bone = metrics["bone_mass_kg"]
            if (bone != null && HealthPermission.getWritePermission(BoneMassRecord::class) in granted) {
                add(
                    RecordDraft("bone_mass", "BoneMassRecord", BoneMassRecord::class) { meta ->
                        BoneMassRecord(time, zoneOffset, Mass.kilograms(bone), meta)
                    },
                )
            }
            val tbw = metrics["tbw_kg"]
            if (tbw != null && HealthPermission.getWritePermission(BodyWaterMassRecord::class) in granted) {
                add(
                    RecordDraft("body_water", "BodyWaterMassRecord", BodyWaterMassRecord::class) { meta ->
                        BodyWaterMassRecord(time, zoneOffset, Mass.kilograms(tbw), meta)
                    },
                )
            }
            val bmr = metrics["bmr_kcal"]
            if (bmr != null && HealthPermission.getWritePermission(BasalMetabolicRateRecord::class) in granted) {
                add(
                    RecordDraft("bmr", "BasalMetabolicRateRecord", BasalMetabolicRateRecord::class) { meta ->
                        BasalMetabolicRateRecord(time, zoneOffset, Power.kilocaloriesPerDay(bmr), meta)
                    },
                )
            }
            val hr = measurement.heartRateBpm
            if (hr != null && HealthPermission.getWritePermission(HeartRateRecord::class) in granted) {
                add(
                    RecordDraft("heart_rate", "HeartRateRecord", HeartRateRecord::class) { meta ->
                        HeartRateRecord(
                            startTime = time,
                            startZoneOffset = zoneOffset,
                            endTime = time.plusSeconds(1),
                            endZoneOffset = zoneOffset,
                            samples = listOf(HeartRateRecord.Sample(time, hr.toLong())),
                            metadata = meta,
                        )
                    },
                )
            }
            if (HealthPermission.getWritePermission(HeightRecord::class) in granted) {
                add(
                    RecordDraft("height", "HeightRecord", HeightRecord::class) { meta ->
                        HeightRecord(time, zoneOffset, Length.meters(profile.heightCm / 100.0), meta)
                    },
                )
            }
        }

        if (drafts.isEmpty()) {
            return HealthConnectWriteSummary(0, 0, "No Health Connect write permissions granted")
        }

        val ledgerDao = db.healthConnectLedgerDao()
        val nowMs = System.currentTimeMillis()
        val ledgerRows = mutableListOf<HealthConnectSyncLedgerEntity>()
        val records = drafts.map { draft ->
            val cid = clientRecordId(measurement.id, draft.tag)
            val existing = ledgerDao.getByClientRecordId(cid)
            val nextVer = (existing?.clientRecordVersion ?: 0L) + 1L
            val meta = Metadata.autoRecorded(
                device = device,
                clientRecordId = cid,
                clientRecordVersion = nextVer,
            )
            ledgerRows += HealthConnectSyncLedgerEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                accountId = measurement.accountId,
                measurementId = measurement.id,
                hcRecordType = draft.typeName,
                clientRecordId = cid,
                clientRecordVersion = nextVer,
                hcRecordUid = existing?.hcRecordUid,
                syncStatus = "pending",
                syncedAtMs = null,
                updatedAtMs = nowMs,
            )
            draft.build(meta)
        }

        return runCatching {
            val uids = platformClient.insertRecords(records)
            val updatedRows = ledgerRows.mapIndexed { idx, row ->
                row.copy(
                    hcRecordUid = uids.getOrNull(idx) ?: row.hcRecordUid,
                    syncStatus = "synced",
                    lastError = null,
                    syncedAtMs = nowMs,
                    updatedAtMs = nowMs,
                )
            }
            ledgerDao.upsertAll(updatedRows)
            HealthConnectWriteSummary(records.size, updatedRows.size)
        }.getOrElse { err ->
            val failedRows = ledgerRows.map { row ->
                row.copy(
                    syncStatus = "failed",
                    lastError = err.message ?: "Insert failed",
                    updatedAtMs = nowMs,
                )
            }
            ledgerDao.upsertAll(failedRows)
            HealthConnectWriteSummary(records.size, 0, err.message)
        }
    }

    /**
     * Prevents orphaned Health Connect records when reassigning a measurement across household
     * members (`INV-05`): deletes any previously synced Member A records BEFORE reassigning and
     * recalculating metrics for Member B, then writes Member B's metrics ONLY if Member B is Primary HC.
     */
    suspend fun reassignMeasurementAndSyncHealthConnect(
        measurementId: String,
        targetProfileId: String,
        scaleModelName: String = "Cult Smart Scale",
    ): MeasurementEntity? {
        deleteSyncedRecordsForMeasurement(measurementId)
        val updated = repository.confirmOrReassignMeasurement(measurementId, targetProfileId) ?: return null
        val targetProfile = repository.getProfile(targetProfileId) ?: return updated
        if (targetProfile.isPrimaryHcUser) {
            syncMeasurement(updated, targetProfile, scaleModelName)
        }
        return updated
    }

    suspend fun deleteSyncedRecordsForMeasurement(measurementId: String) {
        val ledgerDao = db.healthConnectLedgerDao()
        val existing = ledgerDao.getForMeasurement(measurementId).filter { it.syncStatus == "synced" }
        if (existing.isEmpty()) return

        val typeMap: Map<String, KClass<out Record>> = mapOf(
            "WeightRecord" to WeightRecord::class,
            "BodyFatRecord" to BodyFatRecord::class,
            "LeanBodyMassRecord" to LeanBodyMassRecord::class,
            "BoneMassRecord" to BoneMassRecord::class,
            "BodyWaterMassRecord" to BodyWaterMassRecord::class,
            "BasalMetabolicRateRecord" to BasalMetabolicRateRecord::class,
            "HeartRateRecord" to HeartRateRecord::class,
            "HeightRecord" to HeightRecord::class,
        )

        existing.groupBy { it.hcRecordType }.forEach { (typeName, rows) ->
            val kClass = typeMap[typeName] ?: return@forEach
            runCatching {
                platformClient.deleteRecordsByClientId(kClass, rows.map { it.clientRecordId })
            }
        }
        val now = System.currentTimeMillis()
        ledgerDao.upsertAll(existing.map { it.copy(syncStatus = "deleted", updatedAtMs = now) })
    }

    suspend fun readTodayContext(
        date: LocalDate = LocalDate.now(),
        includeRestingHr: Boolean = false,
    ): DailyHealthContext =
        if (platformClient.getAvailability() == HealthConnectAvailability.AVAILABLE) {
            runCatching { platformClient.readDailyContext(date, includeRestingHr) }
                .getOrElse { DailyHealthContext(date, null, null, null) }
        } else {
            DailyHealthContext(date, null, null, null)
        }

    suspend fun pollExternalWeights(currentToken: String?): ExternalChangesBatch? {
        if (platformClient.getAvailability() != HealthConnectAvailability.AVAILABLE) return null
        val tok = currentToken?.takeIf { it.isNotBlank() } ?: platformClient.getWeightChangesToken()
        if (tok.isBlank()) return null
        return platformClient.fetchExternalWeightChanges(tok, context.packageName)
    }

    /**
     * Fetches external `WeightRecord` changes (`UpsertionChange` & `DeletionChange`), persists imported
     * weights to Room as `source = "health_connect_import"`, and applies soft deletes (`INV-16`).
     */
    suspend fun pollAndImportExternalWeights(
        currentToken: String?,
        accountId: String,
    ): ExternalSyncApplyResult? {
        val batch = pollExternalWeights(currentToken) ?: return null
        val profiles = db.householdProfileDao().getActiveProfiles()
        val target = profiles.firstOrNull { it.isPrimaryHcUser } ?: profiles.firstOrNull()
        var imported = 0
        var deleted = 0

        if (target != null) {
            batch.importedWeights.forEach { ext ->
                repository.upsertExternalHealthConnectWeight(
                    accountId = accountId,
                    targetProfile = target,
                    externalRecordId = ext.recordId,
                    weightKg = ext.weightKg,
                    measuredAtMs = ext.measuredAtMs,
                )
                imported++
            }
        }

        batch.deletedExternalIds.forEach { extId ->
            val detId = OpenCultRepository.externalImportUuid(extId)
            val existing = db.measurementDao().getById(detId)
            if (existing != null && existing.deletedAtMs == null) {
                repository.softDeleteMeasurement(detId)
                deleted++
            }
        }
        return ExternalSyncApplyResult(batch.nextToken, imported, deleted)
    }

    suspend fun backfillRecentMeasurements(profileId: String, daysBack: Int = 90): Int {
        val profile = repository.getProfile(profileId) ?: return 0
        if (!profile.isPrimaryHcUser) return 0
        val cutoffMs = System.currentTimeMillis() - daysBack * 86_400_000L
        val measurements = db.measurementDao().getRecentForProfile(profileId, limit = 500)
            .filter { it.measuredAtMs >= cutoffMs && it.assignmentState == "confirmed" && it.source != "health_connect_import" }
        var written = 0
        measurements.forEach { m ->
            val res = syncMeasurement(m, profile)
            if (res.writtenRecords > 0) written++
        }
        return written
    }
}
