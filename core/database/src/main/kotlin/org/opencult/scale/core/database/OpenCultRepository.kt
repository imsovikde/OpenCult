package org.opencult.scale.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.opencult.scale.core.bia.ActivityLevel
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.DualFreqImpedance
import org.opencult.scale.core.bia.FullBodyCompositionReport
import org.opencult.scale.core.bia.MetricProvenance
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.database.entity.HealthConnectSyncLedgerEntity
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.database.entity.RawBlePacketEntity
import org.opencult.scale.core.database.entity.ScaleDeviceEntity
import org.opencult.scale.core.database.entity.SyncOutboxEntity
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RawPacketDraft(
    val seqNo: Int,
    val characteristicUuid: String,
    val packetTypeHex: String,
    val payloadHex: String,
    val checksumValid: Boolean?,
    val decodedImpedancesOhm: List<Double>,
    val capturedAtMs: Long,
)

@Singleton
class OpenCultRepository @Inject constructor(
    private val db: OpenCultDatabase,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeActiveProfiles(): Flow<List<HouseholdProfileEntity>> =
        db.householdProfileDao().observeActiveProfiles()

    fun observeMeasurementsForProfile(profileId: String): Flow<List<MeasurementEntity>> =
        db.measurementDao().observeByProfile(profileId)

    fun observeAllMeasurements(): Flow<List<MeasurementEntity>> =
        db.measurementDao().observeAllActive()

    fun observeActiveDatesForProfile(profileId: String): Flow<List<String>> =
        db.measurementDao().observeActiveDatesForProfile(profileId)

    fun observeDevices(): Flow<List<ScaleDeviceEntity>> =
        db.scaleDeviceDao().observeDevices()

    fun observeLatestRawPackets(limit: Int = 40): Flow<List<RawBlePacketEntity>> =
        db.rawBlePacketDao().observeLatest(limit)

    fun observePendingOutboxCount(): Flow<Int> =
        db.syncOutboxDao().observePendingCount()

    fun observeHealthConnectLedger(limit: Int = 50): Flow<List<HealthConnectSyncLedgerEntity>> =
        db.healthConnectLedgerDao().observeRecent(limit)

    suspend fun getProfileCount(): Int = db.householdProfileDao().countActiveProfiles()

    suspend fun getProfile(id: String): HouseholdProfileEntity? = db.householdProfileDao().getById(id)

    suspend fun getMeasurement(id: String): MeasurementEntity? = db.measurementDao().getById(id)

    suspend fun getPacketsForMeasurement(measurementId: String): List<RawBlePacketEntity> =
        db.rawBlePacketDao().getForMeasurement(measurementId)

    suspend fun createOrUpdateProfile(profile: HouseholdProfileEntity) {
        db.withTransaction {
            val existing = db.householdProfileDao().getById(profile.id)
            if (existing == null) {
                val activeCount = db.householdProfileDao().countActiveProfiles()
                require(activeCount < HouseholdMatcher.MAX_HOUSEHOLD_PROFILES) {
                    "Household profile cap (${HouseholdMatcher.MAX_HOUSEHOLD_PROFILES}) reached."
                }
            }
            db.householdProfileDao().upsert(profile)
            val mutationId = UUID.randomUUID().toString()
            db.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "profile",
                    entityId = profile.id,
                    operation = "UPSERT",
                    payloadJson = json.encodeToString(
                        mapOf(
                            "id" to profile.id,
                            "account_id" to profile.accountId,
                            "display_name" to profile.displayName,
                            "avatar_color" to profile.avatarColor,
                            "biological_sex" to profile.biologicalSex,
                            "birth_date" to profile.birthDate,
                            "height_cm" to profile.heightCm.toString(),
                            "activity_level" to profile.activityLevel,
                            "athlete_mode" to profile.athleteMode.toString(),
                            "auto_match_window_kg" to profile.autoMatchWindowKg.toString(),
                            "target_weight_kg" to (profile.targetWeightKg?.toString() ?: ""),
                            "target_body_fat_pct" to (profile.targetBodyFatPct?.toString() ?: ""),
                            "is_primary_hc_user" to profile.isPrimaryHcUser.toString(),
                            "sort_order" to profile.sortOrder.toString(),
                        ),
                    ),
                    mutationId = mutationId,
                    createdAtMs = profile.updatedAtMs,
                    nextAttemptAtMs = profile.updatedAtMs,
                ),
            )
        }
    }

    suspend fun ensureDefaultProfileIfEmpty(accountId: String): HouseholdProfileEntity {
        val existing = db.householdProfileDao().getActiveProfiles()
        if (existing.isNotEmpty()) return existing.first()
        val now = System.currentTimeMillis()
        val profile = HouseholdProfileEntity(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            displayName = "Primary Member",
            avatarColor = "#2185F7",
            biologicalSex = "male",
            birthDate = "1995-01-01",
            heightCm = 176.0,
            activityLevel = "moderate",
            athleteMode = false,
            autoMatchWindowKg = 2.50,
            targetWeightKg = 72.0,
            targetBodyFatPct = 15.0,
            isPrimaryHcUser = true,
            sortOrder = 0,
            createdAtMs = now,
            updatedAtMs = now,
        )
        createOrUpdateProfile(profile)
        return profile
    }

    suspend fun upsertScaleDevice(device: ScaleDeviceEntity) {
        db.withTransaction {
            db.scaleDeviceDao().upsert(device)
            db.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "device",
                    entityId = device.id,
                    operation = "UPSERT",
                    payloadJson = device.id,
                    mutationId = UUID.randomUUID().toString(),
                    createdAtMs = device.updatedAtMs,
                    nextAttemptAtMs = device.updatedAtMs,
                ),
            )
        }
    }

    /**
     * Atomically commits a locked weigh-in, raw packets, Health Connect pending ledger rows,
     * and Supabase outbox item inside a single Room SQLite transaction (`INV-06` < 50 ms).
     */
    suspend fun commitMeasurementAtomic(
        accountId: String,
        targetProfileId: String,
        assignmentState: String,
        scaleDeviceId: String?,
        report: FullBodyCompositionReport,
        measuredAtMs: Long,
        imp50KhzOhm: Double?,
        impHighOhm: Double?,
        rawPackets: List<RawPacketDraft>,
        notes: String? = null,
        explicitMeasurementId: String? = null,
    ): MeasurementEntity {
        val measurementId = explicitMeasurementId ?: UUID.randomUUID().toString()
        val mutationId = UUID.randomUUID().toString()
        val localDate = Instant.ofEpochMilli(measuredAtMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val metricsMap = report.metrics.associate { it.id to it.value }
        val provenanceMap = report.metrics.associate { it.id to it.provenance.name }

        val measurement = MeasurementEntity(
            id = measurementId,
            accountId = accountId,
            profileId = targetProfileId,
            scaleDeviceId = scaleDeviceId,
            measuredAtMs = measuredAtMs,
            localDate = localDate,
            source = report.effectiveTier.sourceWireName,
            assignmentState = assignmentState,
            weightKg = report.weightKg,
            heartRateBpm = report.heartRateBpm,
            impedance50KhzOhm = imp50KhzOhm,
            impedanceHighOhm = impHighOhm,
            metricsJson = json.encodeToString(metricsMap),
            provenanceJson = json.encodeToString(provenanceMap),
            notes = notes,
            mutationId = mutationId,
            createdAtMs = measuredAtMs,
            updatedAtMs = measuredAtMs,
        )

        val packetEntities = rawPackets.map { draft ->
            RawBlePacketEntity(
                id = UUID.randomUUID().toString(),
                accountId = accountId,
                measurementId = measurementId,
                seqNo = draft.seqNo,
                characteristicUuid = draft.characteristicUuid,
                packetTypeHex = draft.packetTypeHex,
                payloadHex = draft.payloadHex,
                checksumValid = draft.checksumValid,
                decodedImpedancesJson = json.encodeToString(draft.decodedImpedancesOhm),
                capturedAtMs = draft.capturedAtMs,
            )
        }

        val outboxItems = buildList {
            add(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "measurement",
                    entityId = measurementId,
                    operation = "UPSERT",
                    payloadJson = measurementId,
                    mutationId = mutationId,
                    createdAtMs = measuredAtMs,
                    nextAttemptAtMs = measuredAtMs,
                ),
            )
            packetEntities.forEach { pkt ->
                add(
                    SyncOutboxEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "raw_packet",
                        entityId = pkt.id,
                        operation = "UPSERT",
                        payloadJson = pkt.id,
                        mutationId = mutationId,
                        createdAtMs = measuredAtMs,
                        nextAttemptAtMs = measuredAtMs,
                    ),
                )
            }
        }

        db.withTransaction {
            db.measurementDao().upsert(measurement)
            if (packetEntities.isNotEmpty()) {
                db.rawBlePacketDao().insertAll(packetEntities)
            }
            db.syncOutboxDao().enqueueAll(outboxItems)
        }
        return measurement
    }

    suspend fun upsertExternalHealthConnectWeight(
        accountId: String,
        targetProfile: HouseholdProfileEntity,
        externalRecordId: String,
        weightKg: Double,
        measuredAtMs: Long,
    ): MeasurementEntity {
        val deterministicId = externalImportUuid(externalRecordId)
        val now = System.currentTimeMillis()
        val mut = UUID.randomUUID().toString()
        val localDate = Instant.ofEpochMilli(measuredAtMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val report = BodyMetricsEngine.calculate(
            anthropometry = targetProfile.toAnthropometry(),
            requestedTier = ScaleTier.MANUAL_OR_IMPORT,
            weightKg = weightKg.coerceIn(2.0, 400.0),
        )
        val metricsMap = report.metrics.associate { it.id to it.value }
        val provenanceMap = report.metrics.associate { it.id to it.provenance.name }

        val existing = db.measurementDao().getById(deterministicId)
        val entity = MeasurementEntity(
            id = deterministicId,
            accountId = accountId,
            profileId = targetProfile.id,
            scaleDeviceId = null,
            measuredAtMs = measuredAtMs,
            localDate = localDate,
            source = "health_connect_import",
            assignmentState = "confirmed",
            weightKg = report.weightKg,
            heartRateBpm = null,
            impedance50KhzOhm = null,
            impedanceHighOhm = null,
            metricsJson = json.encodeToString(metricsMap),
            provenanceJson = json.encodeToString(provenanceMap),
            notes = "Health Connect external import ($externalRecordId)",
            mutationId = mut,
            createdAtMs = existing?.createdAtMs ?: now,
            updatedAtMs = now,
            deletedAtMs = null,
        )
        db.withTransaction {
            db.measurementDao().upsert(entity)
            db.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "measurement",
                    entityId = entity.id,
                    operation = "UPSERT",
                    payloadJson = entity.id,
                    mutationId = mut,
                    createdAtMs = now,
                    nextAttemptAtMs = now,
                ),
            )
        }
        return entity
    }

    /**
     * Reassigns [measurementId] to [profileId] and recalculates all 56 metrics against the target
     * member's sex, age, height, activity level, athlete mode, and target weight while preserving
     * raw hardware packets and tier provenance.
     */
    suspend fun confirmOrReassignMeasurement(measurementId: String, profileId: String): MeasurementEntity? {
        val m = db.measurementDao().getById(measurementId) ?: return null
        val targetProfile = db.householdProfileDao().getById(profileId) ?: return null
        val pkts = db.rawBlePacketDao().getForMeasurement(measurementId)
        val dualFreq = extractDualFreqFromRawPackets(pkts)
        val report = BodyMetricsEngine.recalculateForProfile(
            anthropometry = targetProfile.toAnthropometry(),
            sourceWireName = m.source,
            weightKg = m.weightKg,
            heartRateBpm = m.heartRateBpm,
            impedance50KhzOhm = m.impedance50KhzOhm,
            dualFreqImpedance = dualFreq,
        )

        val metricsJson = json.encodeToString(report.metrics.associate { it.id to it.value })
        val provenanceJson = json.encodeToString(report.metrics.associate { it.id to it.provenance.name })
        val now = System.currentTimeMillis()
        val mut = UUID.randomUUID().toString()

        db.withTransaction {
            db.measurementDao().updateReassignedProfileAndMetrics(
                id = measurementId,
                profileId = profileId,
                metricsJson = metricsJson,
                provenanceJson = provenanceJson,
                mutationId = mut,
                updatedAtMs = now,
            )
            db.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "measurement",
                    entityId = measurementId,
                    operation = "UPSERT",
                    payloadJson = measurementId,
                    mutationId = mut,
                    createdAtMs = now,
                    nextAttemptAtMs = now,
                ),
            )
        }
        return db.measurementDao().getById(measurementId)
    }

    suspend fun recalculateReportFromStored(
        measurement: MeasurementEntity,
        profile: HouseholdProfileEntity?,
    ): FullBodyCompositionReport? {
        if (profile == null) return null
        val pkts = db.rawBlePacketDao().getForMeasurement(measurement.id)
        val dualFreq = extractDualFreqFromRawPackets(pkts)
        return BodyMetricsEngine.recalculateForProfile(
            anthropometry = profile.toAnthropometry(),
            sourceWireName = measurement.source,
            weightKg = measurement.weightKg,
            heartRateBpm = measurement.heartRateBpm,
            impedance50KhzOhm = measurement.impedance50KhzOhm,
            dualFreqImpedance = dualFreq,
        )
    }

    fun extractDualFreqFromRawPackets(packets: List<RawBlePacketEntity>): DualFreqImpedance? {
        val bePackets = packets.filter { it.packetTypeHex.equals("0xBE", ignoreCase = true) }
        if (bePackets.isEmpty()) return null
        var lowList: List<Double>? = null
        var highList: List<Double>? = null
        bePackets.forEach { pkt ->
            val channels = runCatching { json.decodeFromString<List<Double>>(pkt.decodedImpedancesJson) }.getOrNull()
            if (channels != null && channels.size == 5) {
                val hex = pkt.payloadHex.lowercase()
                when {
                    hex.startsWith("be00") -> lowList = channels
                    hex.startsWith("be01") -> highList = channels
                }
            }
        }
        val lo = lowList ?: return null
        val hi = highList ?: return null
        return DualFreqImpedance(
            lowRightArmOhm = lo[0],
            lowLeftArmOhm = lo[1],
            lowTrunkOhm = lo[2],
            lowRightLegOhm = lo[3],
            lowLeftLegOhm = lo[4],
            highRightArmOhm = hi[0],
            highLeftArmOhm = hi[1],
            highTrunkOhm = hi[2],
            highRightLegOhm = hi[3],
            highLeftLegOhm = hi[4],
        )
    }

    suspend fun softDeleteMeasurement(measurementId: String) {
        val now = System.currentTimeMillis()
        val mut = UUID.randomUUID().toString()
        db.withTransaction {
            db.measurementDao().softDelete(measurementId, mut, now)
            db.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "measurement",
                    entityId = measurementId,
                    operation = "DELETE",
                    payloadJson = measurementId,
                    mutationId = mut,
                    createdAtMs = now,
                    nextAttemptAtMs = now,
                ),
            )
        }
    }

    suspend fun softDeleteProfile(profileId: String) {
        val now = System.currentTimeMillis()
        val mut = UUID.randomUUID().toString()
        db.withTransaction {
            db.householdProfileDao().softDelete(profileId, now)
            db.syncOutboxDao().enqueue(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "profile",
                    entityId = profileId,
                    operation = "DELETE",
                    payloadJson = profileId,
                    mutationId = mut,
                    createdAtMs = now,
                    nextAttemptAtMs = now,
                ),
            )
        }
    }

    suspend fun evaluateMatchForWeight(weightKg: Double): HouseholdMatchOutcome {
        val profiles = db.householdProfileDao().getActiveProfiles()
        val baselines = mutableMapOf<String, Double>()
        profiles.forEach { profile ->
            val recent = db.measurementDao().getRecentForProfile(profile.id, limit = 5)
            if (recent.isNotEmpty()) {
                baselines[profile.id] = recent.map { it.weightKg }.average()
            }
        }
        return HouseholdMatcher.matchProfile(weightKg, profiles, baselines)
    }

    suspend fun bindLocalDataToSignedInAccount(newAccountId: String) {
        db.withTransaction {
            db.measurementDao().claimGuestProfiles(newAccountId)
            db.measurementDao().claimGuestDevices(newAccountId)
            db.measurementDao().claimGuestMeasurements(newAccountId)
        }
    }

    fun HouseholdProfileEntity.toAnthropometry(dailyStepsAvg: Int? = null): UserAnthropometry {
        val dob = runCatching { LocalDate.parse(birthDate) }.getOrElse { LocalDate.of(1995, 1, 1) }
        val age = Period.between(dob, LocalDate.now()).years.coerceIn(10, 110)
        val sex = if (biologicalSex.equals("female", ignoreCase = true)) BiologicalSex.FEMALE else BiologicalSex.MALE
        val act = ActivityLevel.entries.firstOrNull { it.name.equals(activityLevel, ignoreCase = true) }
            ?: ActivityLevel.MODERATE
        return UserAnthropometry(
            sex = sex,
            ageYears = age,
            heightCm = heightCm,
            activityLevel = act,
            athleteMode = athleteMode,
            dailyStepsAvg = dailyStepsAvg,
            targetWeightKg = targetWeightKg,
        )
    }

    fun decodeMetricsJson(raw: String): Map<String, Double?> =
        runCatching { json.decodeFromString<Map<String, Double?>>(raw) }.getOrElse { emptyMap() }

    fun decodeProvenanceJson(raw: String): Map<String, MetricProvenance> =
        runCatching {
            json.decodeFromString<Map<String, String>>(raw).mapValues { (_, v) ->
                runCatching { MetricProvenance.valueOf(v) }.getOrDefault(MetricProvenance.ANTHROPOMETRIC_ESTIMATE)
            }
        }.getOrElse { emptyMap() }

    companion object {
        fun externalImportUuid(externalRecordId: String): String =
            UUID.nameUUIDFromBytes("opencult:hc_ext:$externalRecordId".toByteArray(Charsets.UTF_8)).toString()
    }
}
