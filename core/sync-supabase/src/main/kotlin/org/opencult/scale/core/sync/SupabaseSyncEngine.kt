package org.opencult.scale.core.sync

import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.MeasurementEntity
import java.time.Instant
import java.time.ZoneId
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first Supabase Auth + Postgrest + Realtime synchronization engine (`TRD:SYNC`).
 * Owns self-echo `mutation_id` suppression and triggers WorkManager outbox draining.
 */
@Singleton
class SupabaseSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: SupabaseConfig,
    private val vault: SecureVault,
    private val db: OpenCultDatabase,
    private val repository: OpenCultRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeJob: Job? = null

    private val recentLocalMutationIds: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet())

    private val _status = MutableStateFlow(
        SyncStatusSnapshot(lastSyncedAtMs = vault.getLastSyncTimestampMs()),
    )
    val status: StateFlow<SyncStatusSnapshot> = _status.asStateFlow()

    val client: SupabaseClient? by lazy {
        if (!config.isConfigured) return@lazy null
        createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.anonKey,
        ) {
            httpEngine = OkHttp.create()
            install(Auth) {
                scheme = "opencult"
                host = "auth"
                sessionManager = vault
                codeVerifierCache = vault
            }
            install(Postgrest)
            install(Realtime)
        }.also { sb ->
            sb.auth.sessionStatus
                .onEach { sessionState ->
                    when (sessionState) {
                        is SessionStatus.Authenticated -> {
                            val uid = sessionState.session.user?.id
                            val mail = sessionState.session.user?.email
                            if (!uid.isNullOrBlank()) {
                                repository.bindLocalDataToSignedInAccount(uid)
                                startRealtimeChannelIfNeeded(sb)
                            }
                            _status.update {
                                it.copy(
                                    isSignedIn = true,
                                    accountId = uid,
                                    userEmail = mail,
                                    lastError = null,
                                )
                            }
                            enqueueOutboxDrain()
                        }
                        is SessionStatus.NotAuthenticated -> {
                            realtimeJob?.cancel()
                            _status.update {
                                it.copy(
                                    isSignedIn = false,
                                    accountId = null,
                                    userEmail = null,
                                    realtimeConnected = false,
                                )
                            }
                        }
                        else -> Unit
                    }
                }
                .launchIn(scope)
        }
    }

    fun currentAccountIdOrGuest(): String =
        client?.auth?.currentUserOrNull()?.id ?: _status.value.accountId ?: GUEST_ACCOUNT_ID

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val sb = client ?: error("Supabase is not configured in BuildConfig")
        sb.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        Unit
    }.onFailure { err ->
        _status.update { it.copy(lastError = err.message) }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val sb = client ?: error("Supabase is not configured in BuildConfig")
        sb.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        Unit
    }.onFailure { err ->
        _status.update { it.copy(lastError = err.message) }
    }

    suspend fun signOut() {
        runCatching { client?.auth?.signOut() }
        realtimeJob?.cancel()
        _status.update {
            it.copy(isSignedIn = false, accountId = null, userEmail = null, realtimeConnected = false)
        }
    }

    fun handleIncomingIntent(rawIntent: Intent?) {
        val safeIntent = OAuthRedirectSanitizer.sanitizeCallbackIntent(rawIntent) ?: return
        client?.handleDeeplinks(safeIntent)
    }

    fun enqueueOutboxDrain() {
        val req = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "opencult_outbox_drain",
            ExistingWorkPolicy.REPLACE,
            req,
        )
    }

    fun recordSelfMutationId(mutationId: String) {
        synchronized(recentLocalMutationIds) {
            if (recentLocalMutationIds.size > 200) {
                val first = recentLocalMutationIds.firstOrNull()
                if (first != null) recentLocalMutationIds.remove(first)
            }
            recentLocalMutationIds.add(mutationId)
        }
    }

    fun isSelfEchoMutation(mutationId: String): Boolean = recentLocalMutationIds.contains(mutationId)

    suspend fun drainOutboxNow(): Result<Int> = runCatching {
        val sb = client ?: return Result.success(0)
        val uid = sb.auth.currentUserOrNull()?.id ?: return Result.success(0)
        _status.update { it.copy(isSyncing = true, lastError = null) }

        val outboxDao = db.syncOutboxDao()
        val batch = outboxDao.getReadyBatch(limit = 50)
        if (batch.isEmpty()) {
            _status.update { it.copy(isSyncing = false) }
            return Result.success(0)
        }

        val succeededIds = mutableListOf<String>()
        for (item in batch) {
            runCatching {
                recordSelfMutationId(item.mutationId)
                when (item.entityType) {
                    "profile" -> {
                        val entity = db.householdProfileDao().getById(item.entityId) ?: return@runCatching
                        sb.from("household_profiles").upsert(
                            HouseholdProfileDto(
                                id = entity.id,
                                accountId = uid,
                                displayName = entity.displayName,
                                avatarColor = entity.avatarColor,
                                biologicalSex = entity.biologicalSex,
                                birthDate = entity.birthDate,
                                heightCm = entity.heightCm,
                                activityLevel = entity.activityLevel,
                                athleteMode = entity.athleteMode,
                                autoMatchWindowKg = entity.autoMatchWindowKg,
                                targetWeightKg = entity.targetWeightKg,
                                targetBodyFatPct = entity.targetBodyFatPct,
                                isPrimaryHcUser = entity.isPrimaryHcUser,
                                sortOrder = entity.sortOrder,
                                createdAt = Instant.ofEpochMilli(entity.createdAtMs).toString(),
                                updatedAt = Instant.ofEpochMilli(entity.updatedAtMs).toString(),
                                deletedAt = entity.deletedAtMs?.let { Instant.ofEpochMilli(it).toString() },
                            ),
                        )
                    }
                    "device" -> {
                        val dev = db.scaleDeviceDao().getDevices().firstOrNull { it.id == item.entityId }
                            ?: return@runCatching
                        sb.from("scale_devices").upsert(
                            ScaleDeviceDto(
                                id = dev.id,
                                accountId = uid,
                                bleMacOrId = dev.bleMacOrId,
                                advertisedName = dev.advertisedName,
                                modelTier = dev.modelTier,
                                electrodeCount = dev.electrodeCount,
                                firmwareNotes = dev.firmwareNotes,
                                lastBatteryPct = dev.lastBatteryPct,
                                lastSeenAt = dev.lastSeenAtMs?.let { Instant.ofEpochMilli(it).toString() },
                                createdAt = Instant.ofEpochMilli(dev.createdAtMs).toString(),
                                updatedAt = Instant.ofEpochMilli(dev.updatedAtMs).toString(),
                                deletedAt = dev.deletedAtMs?.let { Instant.ofEpochMilli(it).toString() },
                            ),
                        )
                    }
                    "measurement" -> {
                        val m = db.measurementDao().getById(item.entityId) ?: return@runCatching
                        val mJson: JsonElement = json.parseToJsonElement(m.metricsJson)
                        val pJson: JsonElement = json.parseToJsonElement(m.provenanceJson)
                        sb.from("measurements").upsert(
                            MeasurementDto(
                                id = m.id,
                                accountId = uid,
                                profileId = m.profileId,
                                scaleDeviceId = m.scaleDeviceId,
                                measuredAt = Instant.ofEpochMilli(m.measuredAtMs).toString(),
                                source = m.source,
                                assignmentState = m.assignmentState,
                                weightKg = m.weightKg,
                                heartRateBpm = m.heartRateBpm,
                                impedance50KhzOhm = m.impedance50KhzOhm,
                                impedanceHighOhm = m.impedanceHighOhm,
                                metricsJson = mJson,
                                provenanceJson = pJson,
                                notes = m.notes,
                                mutationId = m.mutationId,
                                createdAt = Instant.ofEpochMilli(m.createdAtMs).toString(),
                                updatedAt = Instant.ofEpochMilli(m.updatedAtMs).toString(),
                                deletedAt = m.deletedAtMs?.let { Instant.ofEpochMilli(it).toString() },
                            ),
                        )
                    }
                    "raw_packet" -> {
                        val pkts = db.rawBlePacketDao()
                            .getForMeasurement(
                                db.measurementDao().getAllMeasurementIdFallback(item.entityId) ?: "",
                            )
                        val target = pkts.firstOrNull { it.id == item.entityId }
                        if (target != null) {
                            val imps = runCatching {
                                json.decodeFromString<List<Double>>(target.decodedImpedancesJson)
                            }.getOrDefault(emptyList())
                            sb.from("raw_ble_packets").upsert(
                                RawBlePacketDto(
                                    id = target.id,
                                    accountId = uid,
                                    measurementId = target.measurementId,
                                    seqNo = target.seqNo,
                                    characteristicUuid = target.characteristicUuid,
                                    packetTypeHex = target.packetTypeHex,
                                    payloadHex = target.payloadHex,
                                    checksumValid = target.checksumValid,
                                    decodedImpedancesOhm = imps,
                                    capturedAt = Instant.ofEpochMilli(target.capturedAtMs).toString(),
                                ),
                            )
                        }
                    }
                }
                succeededIds += item.id
            }.onFailure { err ->
                outboxDao.markFailed(
                    id = item.id,
                    error = err.message ?: "Sync failed",
                    nextAttemptMs = System.currentTimeMillis() + 30_000L,
                )
                _status.update { it.copy(lastError = err.message) }
            }
        }
        if (succeededIds.isNotEmpty()) {
            outboxDao.deleteByIds(succeededIds)
            val now = System.currentTimeMillis()
            vault.setLastSyncTimestampMs(now)
            _status.update { it.copy(lastSyncedAtMs = now) }
        }
        _status.update { it.copy(isSyncing = false) }
        succeededIds.size
    }.onFailure { err ->
        _status.update { it.copy(isSyncing = false, lastError = err.message) }
    }

    private fun startRealtimeChannelIfNeeded(sb: SupabaseClient) {
        if (realtimeJob?.isActive == true) return
        realtimeJob = scope.launch {
            runCatching {
                val chan = sb.realtime.channel("opencult_measurements")
                val changes = chan.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "measurements"
                }
                changes.onEach { action ->
                    when (action) {
                        is PostgresAction.Insert -> applyRemoteRecordJson(action.record)
                        is PostgresAction.Update -> applyRemoteRecordJson(action.record)
                        is PostgresAction.Delete -> {
                            val id = action.oldRecord["id"]?.toString()?.trim('"') ?: return@onEach
                            db.measurementDao().softDelete(id, "remote-del", System.currentTimeMillis())
                        }
                        else -> Unit
                    }
                }.launchIn(this)
                chan.subscribe()
                _status.update { it.copy(realtimeConnected = true) }
            }.onFailure { err ->
                _status.update { it.copy(realtimeConnected = false, lastError = err.message) }
            }
        }
    }

    internal suspend fun applyRemoteRecordJson(recordJson: kotlinx.serialization.json.JsonObject) {
        val dto = runCatching {
            json.decodeFromJsonElement(MeasurementDto.serializer(), recordJson)
        }.getOrNull() ?: return
        if (isSelfEchoMutation(dto.mutationId)) return

        val measuredAtMs = runCatching { Instant.parse(dto.measuredAt).toEpochMilli() }.getOrElse { System.currentTimeMillis() }
        val updatedAtMs = runCatching { Instant.parse(dto.updatedAt).toEpochMilli() }.getOrElse { measuredAtMs }
        val existing = db.measurementDao().getById(dto.id)
        if (existing != null && existing.updatedAtMs >= updatedAtMs) return

        val localDate = Instant.ofEpochMilli(measuredAtMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        db.measurementDao().upsert(
            MeasurementEntity(
                id = dto.id,
                accountId = dto.accountId,
                profileId = dto.profileId,
                scaleDeviceId = dto.scaleDeviceId,
                measuredAtMs = measuredAtMs,
                localDate = localDate,
                source = dto.source,
                assignmentState = dto.assignmentState,
                weightKg = dto.weightKg,
                heartRateBpm = dto.heartRateBpm,
                impedance50KhzOhm = dto.impedance50KhzOhm,
                impedanceHighOhm = dto.impedanceHighOhm,
                metricsJson = dto.metricsJson.toString(),
                provenanceJson = dto.provenanceJson.toString(),
                notes = dto.notes,
                mutationId = dto.mutationId,
                createdAtMs = runCatching { Instant.parse(dto.createdAt).toEpochMilli() }.getOrElse { measuredAtMs },
                updatedAtMs = updatedAtMs,
                deletedAtMs = dto.deletedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            ),
        )
    }

    private suspend fun org.opencult.scale.core.database.dao.MeasurementDao.getAllMeasurementIdFallback(
        packetId: String,
    ): String? {
        val all = db.measurementDao().getRecentForProfile(
            db.householdProfileDao().getActiveProfiles().firstOrNull()?.id ?: return null,
            limit = 50,
        )
        for (m in all) {
            if (db.rawBlePacketDao().getForMeasurement(m.id).any { it.id == packetId }) return m.id
        }
        return null
    }

    companion object {
        const val GUEST_ACCOUNT_ID = "local-guest"
    }
}

@HiltWorker
class OutboxSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncEngine: SupabaseSyncEngine,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val outcome = syncEngine.drainOutboxNow()
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }
}
