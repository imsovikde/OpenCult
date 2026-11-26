package org.opencult.scale.feature.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HealthConnectSyncLedgerEntity
import org.opencult.scale.core.database.entity.RawBlePacketEntity
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorRow
import org.opencult.scale.core.designsystem.components.OpenCultPrimaryButton
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.OpenCultTextField
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.openCultMicroPress
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.healthconnect.HealthConnectAvailability
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SecureVault
import org.opencult.scale.core.sync.SupabaseSyncEngine
import org.opencult.scale.core.sync.SyncStatusSnapshot
import javax.inject.Inject

enum class InspectorTab(val label: String) {
    PACKETS("BLE Hex"),
    HEALTH_CONNECT("Health Connect"),
    SUPABASE("Cloud Sync"),
}

data class InspectorUiState(
    val packets: List<RawBlePacketEntity> = emptyList(),
    val hcLedger: List<HealthConnectSyncLedgerEntity> = emptyList(),
    val outboxCount: Int = 0,
    val syncStatus: SyncStatusSnapshot = SyncStatusSnapshot(),
    val restingHrOptIn: Boolean = false,
    val backgroundHcRead: Boolean = false,
)

@HiltViewModel
class InspectorViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    private val db: OpenCultDatabase,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
    private val vault: SecureVault,
    private val activeMemberSession: ActiveMemberSession,
) : ViewModel() {

    val uiState: StateFlow<InspectorUiState> = combine(
        repository.observeLatestRawPackets(40),
        repository.observeHealthConnectLedger(40),
        repository.observePendingOutboxCount(),
        syncEngine.status,
    ) { packets, ledger, outbox, sync ->
        InspectorUiState(
            packets = packets,
            hcLedger = ledger,
            outboxCount = outbox,
            syncStatus = sync,
        )
    }.combine(activeMemberSession.restingHrOptInEnabled) { state, rhr ->
        state.copy(restingHrOptIn = rhr)
    }.combine(activeMemberSession.backgroundHcReadEnabled) { state, bg ->
        state.copy(backgroundHcRead = bg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InspectorUiState())

    fun hcAvailability(): HealthConnectAvailability = healthConnectGateway.availability()

    fun totalHcPermissionsPoolCount(): Int =
        HealthConnectGateway.requestedPermissions(
            includeRestingHr = uiState.value.restingHrOptIn,
            includeBackgroundRead = uiState.value.backgroundHcRead,
        ).size

    suspend fun getGrantedHcPermissionsCount(): Int =
        runCatching { healthConnectGateway.getGrantedPermissions().size }.getOrDefault(0)

    fun setRestingHrOptIn(enabled: Boolean) {
        activeMemberSession.setRestingHrOptInEnabled(enabled)
    }

    fun setBackgroundHcRead(enabled: Boolean) {
        activeMemberSession.setBackgroundHcReadEnabled(enabled)
    }

    suspend fun signIn(email: String, pass: String): Result<Unit> =
        syncEngine.signInWithEmail(email, pass)

    suspend fun signUp(email: String, pass: String): Result<Unit> =
        syncEngine.signUpWithEmail(email, pass)

    suspend fun signOut() {
        syncEngine.signOut()
    }

    suspend fun drainOutboxNow(): Result<Int> = syncEngine.drainOutboxNow()

    suspend fun fullCloudSyncNow(): Result<Pair<Int, Int>> = runCatching {
        val pushed = syncEngine.drainOutboxNow().getOrThrow()
        val pulled = syncEngine.pullRemoteChanges().getOrThrow()
        pushed to pulled
    }

    suspend fun pollAndImportExternalHc(): String {
        val result = healthConnectGateway.pollAndImportExternalWeights(
            currentToken = vault.getHealthConnectToken(),
            accountId = syncEngine.currentAccountIdOrGuest(),
        ) ?: return "Health Connect unavailable"
        if (result.nextToken.isNotBlank()) {
            vault.setHealthConnectToken(result.nextToken)
        }
        syncEngine.enqueueOutboxDrain()
        return "Imported ${result.importedCount} external weights, soft-deleted ${result.deletedCount}."
    }

    suspend fun backfillPrimaryHistory90d(): String {
        val profiles = db.householdProfileDao().getActiveProfiles()
        val primary = profiles.firstOrNull { it.isPrimaryHcUser }
            ?: return "No household member marked Primary HC User."
        val written = healthConnectGateway.backfillRecentMeasurements(primary.id, daysBack = 90)
        return "Synced $written measurements for ${primary.displayName} (idempotent)."
    }
}

@Composable
fun InspectorScreen(
    onBack: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InspectorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = OpenCultTheme.colors
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var tab by remember { mutableStateOf(InspectorTab.PACKETS) }
    var grantedHcCount by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.restingHrOptIn, state.backgroundHcRead) {
        grantedHcCount = viewModel.getGrantedHcPermissionsCount()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 14.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier
                    .openCultMicroPress(role = Role.Button) { onBack() }
                    .padding(10.dp),
            )
            Text(
                text = "Protocol & Sync Hub",
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
        }

        OpenCultPillSelectorRow(
            options = persistentListOf(
                PillOption(InspectorTab.PACKETS, "BLE Hex"),
                PillOption(InspectorTab.HEALTH_CONNECT, "Health Connect"),
                PillOption(InspectorTab.SUPABASE, "Supabase Cloud"),
            ),
            selected = tab,
            onSelect = { tab = it },
        )

        feedback?.let { msg ->
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.cobaltSubtle) {
                Text(msg, style = OpenCultTheme.typography.body, color = colors.brandCobalt)
            }
        }

        when (tab) {
            InspectorTab.PACKETS -> {
                if (state.packets.isEmpty()) {
                    OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No raw BLE frames recorded yet. Every 0xCF, 0xBE, and opaque 0xDF frame captured during weigh-ins appears verbatim here.",
                            style = OpenCultTheme.typography.body,
                            color = colors.textSecondary,
                        )
                    }
                } else {
                    state.packets.forEach { pkt ->
                        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "#${pkt.seqNo}  ${pkt.packetTypeHex}  (${pkt.characteristicUuid})",
                                    style = OpenCultTheme.typography.buttonLabel,
                                    color = colors.textPrimary,
                                )
                                OpenCultStatusChip(
                                    label = when (pkt.checksumValid) {
                                        true -> "XOR OK"
                                        false -> "XOR BAD"
                                        null -> "Opaque"
                                    },
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = pkt.payloadHex,
                                style = OpenCultTheme.typography.monoHex,
                                color = colors.textSecondary,
                            )
                            if (pkt.packetTypeHex == "0xDF") {
                                Text(
                                    text = "0xDF proprietary encrypted payload stored verbatim (unverified without vendor trace).",
                                    style = OpenCultTheme.typography.microLabel,
                                    color = colors.textMuted,
                                )
                            } else if (pkt.decodedImpedancesJson != "[]") {
                                Text(
                                    text = "Impedance (ohm): ${pkt.decodedImpedancesJson}",
                                    style = OpenCultTheme.typography.monoHex,
                                    color = colors.brandCobalt,
                                )
                            }
                        }
                    }
                }
            }

            InspectorTab.HEALTH_CONNECT -> {
                OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Availability: ${viewModel.hcAvailability().name}",
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Granted permissions: $grantedHcCount / ${viewModel.totalHcPermissionsPoolCount()}",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OpenCultSecondaryButton(
                            text = "Grant Perms",
                            onClick = onRequestHealthConnectPermissions,
                            modifier = Modifier.weight(1f),
                        )
                        OpenCultSecondaryButton(
                            text = "Import Changes",
                            onClick = {
                                scope.launch {
                                    feedback = viewModel.pollAndImportExternalHc()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OpenCultPrimaryButton(
                        text = "Backfill 90-Day History",
                        onClick = {
                            scope.launch {
                                feedback = viewModel.backfillPrimaryHistory90d()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Optional Opt-In Read Scopes", style = OpenCultTheme.typography.sectionTitle, color = colors.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Read Resting Heart Rate", style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                            Text("Include external RestingHeartRateRecord in daily context.", style = OpenCultTheme.typography.body, color = colors.textSecondary)
                        }
                        Switch(checked = state.restingHrOptIn, onCheckedChange = { viewModel.setRestingHrOptIn(it) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Health Read", style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                            Text("Allow WorkManager background Health Connect reads.", style = OpenCultTheme.typography.body, color = colors.textSecondary)
                        }
                        Switch(checked = state.backgroundHcRead, onCheckedChange = { viewModel.setBackgroundHcRead(it) })
                    }
                }

                state.hcLedger.forEach { row ->
                    OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.hcRecordType, style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                            OpenCultStatusChip(label = "${row.syncStatus} v${row.clientRecordVersion}")
                        }
                        Text(row.clientRecordId, style = OpenCultTheme.typography.monoHex, color = colors.textMuted)
                    }
                }
            }

            InspectorTab.SUPABASE -> {
                OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (!state.syncStatus.backendConfigured) {
                            "Supabase URL / Key Not Configured"
                        } else if (state.syncStatus.isSignedIn) {
                            "Signed in: ${state.syncStatus.userEmail ?: state.syncStatus.accountId}"
                        } else {
                            "Offline-First Guest Mode (Ready to Sign In)"
                        },
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Pending outbox items: ${state.outboxCount}  |  Realtime: ${if (state.syncStatus.realtimeConnected) "Connected" else "Idle"}",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                    state.syncStatus.lastError?.let { err ->
                        Spacer(Modifier.height(4.dp))
                        Text("Last error: $err", style = OpenCultTheme.typography.body, color = colors.coralAlert)
                    }
                    Spacer(Modifier.height(10.dp))

                    if (!state.syncStatus.isSignedIn) {
                        OpenCultTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            placeholder = "you@example.com",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        OpenCultTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            placeholder = "Password",
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OpenCultPrimaryButton(
                                text = "Sign In",
                                onClick = {
                                    scope.launch {
                                        viewModel.signIn(email, password)
                                            .onSuccess { feedback = "Signed in to Supabase." }
                                            .onFailure { feedback = it.message }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            OpenCultSecondaryButton(
                                text = "Create Account",
                                onClick = {
                                    scope.launch {
                                        viewModel.signUp(email, password)
                                            .onSuccess { feedback = "Account created; confirm email if verification enabled." }
                                            .onFailure { feedback = it.message }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OpenCultPrimaryButton(
                                text = "Sync Push+Pull",
                                onClick = {
                                    scope.launch {
                                        viewModel.fullCloudSyncNow()
                                            .onSuccess { (pushed, pulled) ->
                                                feedback = "Synced: pushed $pushed, pulled $pulled records."
                                            }
                                            .onFailure { feedback = it.message }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            OpenCultSecondaryButton(
                                text = "Sign Out",
                                onClick = {
                                    scope.launch {
                                        viewModel.signOut()
                                        feedback = "Signed out."
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
