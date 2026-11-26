package org.opencult.scale.feature.profiles

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.opencult.scale.core.audio.OpenCultSoundCue
import org.opencult.scale.core.audio.OpenCultSoundEngine
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.database.HouseholdMatcher
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.database.entity.MeasurementEntity
import org.opencult.scale.core.database.entity.ScaleDeviceEntity
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorRow
import org.opencult.scale.core.designsystem.components.OpenCultPrimaryButton
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.components.OpenCultSectionHeader
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.OpenCultTextField
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.dockScrollClearance
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SupabaseSyncEngine
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

private val exportJsonFormat = Json { prettyPrint = true }

@Serializable
data class LosslessExportBundle(
    val exportedAtIso: String,
    val profiles: List<ExportProfileRow>,
    val measurements: List<ExportMeasurementRow>,
)

@Serializable
data class ExportProfileRow(
    val id: String,
    val displayName: String,
    val biologicalSex: String,
    val birthDate: String,
    val heightCm: Double,
    val activityLevel: String,
    val autoMatchWindowKg: Double,
)

@Serializable
data class ExportMeasurementRow(
    val id: String,
    val profileId: String,
    val measuredAtMs: Long,
    val source: String,
    val assignmentState: String,
    val weightKg: Double,
    val heartRateBpm: Int?,
    val metricsJson: String,
    val provenanceJson: String,
    val rawBlePacketsHex: List<String>,
)

data class ProfilesUiState(
    val profiles: List<HouseholdProfileEntity> = emptyList(),
    val activeProfileId: String? = null,
    val memberLockEnabled: Boolean = false,
    val soundFeedbackEnabled: Boolean = true,
    val devices: List<ScaleDeviceEntity> = emptyList(),
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    private val db: OpenCultDatabase,
    private val activeMemberSession: ActiveMemberSession,
    private val soundEngine: OpenCultSoundEngine,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
) : ViewModel() {

    val uiState: StateFlow<ProfilesUiState> = combine(
        repository.observeActiveProfiles(),
        repository.observeDevices(),
        activeMemberSession.activeProfileId,
        activeMemberSession.memberLockEnabled,
        activeMemberSession.soundFeedbackEnabled,
    ) { profiles, devices, activeId, locked, soundEnabled ->
        ProfilesUiState(
            profiles = profiles,
            activeProfileId = activeId ?: profiles.firstOrNull()?.id,
            memberLockEnabled = locked,
            soundFeedbackEnabled = soundEnabled,
            devices = devices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfilesUiState())

    fun selectActiveMember(id: String) {
        activeMemberSession.selectActiveProfile(id)
    }

    fun setMemberLockEnabled(enabled: Boolean) {
        activeMemberSession.setMemberLockEnabled(enabled)
    }

    fun setSoundFeedbackEnabled(enabled: Boolean) {
        activeMemberSession.setSoundFeedbackEnabled(enabled)
        soundEngine.isEnabled = enabled
        if (enabled) {
            soundEngine.play(OpenCultSoundCue.SCALE_CONNECTED)
        }
    }

    suspend fun getHcGrantedCount(): Int =
        runCatching { healthConnectGateway.getGrantedPermissions().size }.getOrDefault(0)

    suspend fun addMember(
        displayName: String,
        sex: BiologicalSex,
        heightCm: Double,
        autoMatchWindowKg: Double,
        isPrimaryHc: Boolean,
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val count = repository.getProfileCount()
        require(count < HouseholdMatcher.MAX_HOUSEHOLD_PROFILES) {
            "Maximum ${HouseholdMatcher.MAX_HOUSEHOLD_PROFILES} household members allowed."
        }
        val profile = HouseholdProfileEntity(
            id = UUID.randomUUID().toString(),
            accountId = syncEngine.currentAccountIdOrGuest(),
            displayName = displayName.trim().ifBlank { "Member ${count + 1}" }.take(60),
            avatarColor = if (sex == BiologicalSex.FEMALE) "#E98258" else "#2185F7",
            biologicalSex = if (sex == BiologicalSex.FEMALE) "female" else "male",
            birthDate = "1995-01-01",
            heightCm = heightCm.coerceIn(50.0, 260.0),
            activityLevel = "moderate",
            athleteMode = false,
            autoMatchWindowKg = autoMatchWindowKg.coerceIn(0.2, 15.0),
            isPrimaryHcUser = isPrimaryHc,
            sortOrder = count,
            createdAtMs = now,
            updatedAtMs = now,
        )
        repository.createOrUpdateProfile(profile)
        activeMemberSession.selectActiveProfile(profile.id)
        syncEngine.enqueueOutboxDrain()
    }

    suspend fun exportLosslessJsonToFile(context: Context): File = withContext(Dispatchers.IO) {
        val profiles = db.householdProfileDao().getActiveProfiles()
        val allMeasurements = db.measurementDao().getRecentForProfile(
            profiles.firstOrNull()?.id ?: "",
            limit = 1000,
        ).let { first ->
            val rest = profiles.drop(1).flatMap { db.measurementDao().getRecentForProfile(it.id, limit = 1000) }
            first + rest
        }

        val bundle = LosslessExportBundle(
            exportedAtIso = Instant.now().toString(),
            profiles = profiles.map {
                ExportProfileRow(
                    id = it.id,
                    displayName = it.displayName,
                    biologicalSex = it.biologicalSex,
                    birthDate = it.birthDate,
                    heightCm = it.heightCm,
                    activityLevel = it.activityLevel,
                    autoMatchWindowKg = it.autoMatchWindowKg,
                )
            },
            measurements = allMeasurements.map { m: MeasurementEntity ->
                val rawHex = db.rawBlePacketDao().getForMeasurement(m.id).map { "${it.packetTypeHex}:${it.payloadHex}" }
                ExportMeasurementRow(
                    id = m.id,
                    profileId = m.profileId,
                    measuredAtMs = m.measuredAtMs,
                    source = m.source,
                    assignmentState = m.assignmentState,
                    weightKg = m.weightKg,
                    heartRateBpm = m.heartRateBpm,
                    metricsJson = m.metricsJson,
                    provenanceJson = m.provenanceJson,
                    rawBlePacketsHex = rawHex,
                )
            },
        )
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "opencult-export-${System.currentTimeMillis()}.json")
        file.writeText(exportJsonFormat.encodeToString(bundle))
        file
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    currentThemeMode: OpenCultThemeMode,
    onChangeThemeMode: (OpenCultThemeMode) -> Unit,
    onOpenInspector: () -> Unit,
    appVersionLabel: String = "v0.1.0",
    modifier: Modifier = Modifier,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = OpenCultTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var showAddSheet by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newHeight by remember { mutableStateOf("170") }
    var newSex by remember { mutableStateOf(BiologicalSex.FEMALE) }
    var newWindow by remember { mutableStateOf("2.5") }
    var newPrimaryHc by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var hcCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        hcCount = viewModel.getHcGrantedCount()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 14.dp)
            .dockScrollClearance(),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Household & Settings",
                style = OpenCultTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            OpenCultStatusChip(label = "${state.profiles.size}/${HouseholdMatcher.MAX_HOUSEHOLD_PROFILES}")
        }

        statusMessage?.let { msg ->
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.cobaltSubtle) {
                Text(msg, style = OpenCultTheme.typography.body, color = colors.brandCobalt)
            }
        }

        OpenCultSectionHeader(title = "Household Members")
        state.profiles.forEach { profile ->
            val isActive = profile.id == state.activeProfileId
            OpenCultBentoCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (isActive) colors.cobaltSubtle else colors.cardSurface,
                borderColor = if (isActive) colors.brandCobalt else colors.borderSubtle,
                onClick = { viewModel.selectActiveMember(profile.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.displayName, style = OpenCultTheme.typography.sectionTitle, color = colors.textPrimary)
                        Text(
                            text = "${profile.heightCm.toInt()} cm  |  Window +/-${profile.autoMatchWindowKg} kg",
                            style = OpenCultTheme.typography.body,
                            color = colors.textSecondary,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (profile.isPrimaryHcUser) {
                            OpenCultStatusChip(label = "Primary HC")
                        }
                        if (isActive) {
                            OpenCultStatusChip(label = "Active")
                        }
                    }
                }
            }
        }

        OpenCultPrimaryButton(
            text = "Add Household Member",
            enabled = state.profiles.size < HouseholdMatcher.MAX_HOUSEHOLD_PROFILES,
            onClick = { showAddSheet = true },
            modifier = Modifier.fillMaxWidth(),
        )

        OpenCultSectionHeader(title = "Audio & Weighing Controls")
        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Clinical Audio Cues", style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                    Text(
                        text = "Calm chime tones for lock, ticks, and save (respects Silent/Vibrate ringer).",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
                Switch(
                    checked = state.soundFeedbackEnabled,
                    onCheckedChange = { viewModel.setSoundFeedbackEnabled(it) },
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lock Weigh-Ins to Active Member", style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                    Text(
                        text = "Send new weigh-ins directly to the selected active member.",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
                Switch(
                    checked = state.memberLockEnabled,
                    onCheckedChange = { viewModel.setMemberLockEnabled(it) },
                )
            }
        }

        OpenCultSectionHeader(title = "Appearance & Theme")
        OpenCultPillSelectorRow(
            options = persistentListOf(
                PillOption(OpenCultThemeMode.DAYLIGHT, "Daylight"),
                PillOption(OpenCultThemeMode.OBSIDIAN, "Obsidian"),
                PillOption(OpenCultThemeMode.SYSTEM, "System"),
            ),
            selected = currentThemeMode,
            onSelect = onChangeThemeMode,
        )

        OpenCultSectionHeader(title = "Paired Scales (${state.devices.size})")
        if (state.devices.isEmpty()) {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Text("No physical Cult scale paired yet. Scan from the + Measure dock button.", style = OpenCultTheme.typography.body, color = colors.textSecondary)
            }
        } else {
            state.devices.forEach { dev ->
                OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(dev.advertisedName, style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
                            Text("${dev.bleMacOrId}  |  ${dev.electrodeCount}E", style = OpenCultTheme.typography.monoHex, color = colors.textMuted)
                        }
                        dev.lastBatteryPct?.let { OpenCultStatusChip(label = "$it%") }
                    }
                }
            }
        }

        OpenCultSectionHeader(title = "Lossless Export & Sync Hub")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpenCultSecondaryButton(
                text = "Share JSON Export",
                onClick = {
                    scope.launch {
                        val file = viewModel.exportLosslessJsonToFile(context)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        statusMessage = "Export written: ${file.name}"
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "Export OpenCult Data"))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            OpenCultSecondaryButton(
                text = "Sync Hub ($hcCount HC)",
                onClick = onOpenInspector,
                modifier = Modifier.weight(1f),
            )
        }

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.subtlePillSurface) {
            Text("About OpenCult", style = OpenCultTheme.typography.buttonLabel, color = colors.textPrimary)
            Text(
                text = "OpenCult $appVersionLabel (GPL-3.0-or-later). Independent open-source interoperability companion; not affiliated with Curefit or Cult.fit.",
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
            )
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = colors.elevatedSurface,
            shape = OpenCultTheme.shapes.sheetSquircle,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("New Household Member", style = OpenCultTheme.typography.sectionTitle, color = colors.textPrimary)
                OpenCultTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = "Display Name",
                    placeholder = "e.g. Anita",
                    modifier = Modifier.fillMaxWidth(),
                )
                OpenCultPillSelectorRow(
                    options = persistentListOf(
                        PillOption(BiologicalSex.MALE, "Male"),
                        PillOption(BiologicalSex.FEMALE, "Female"),
                    ),
                    selected = newSex,
                    onSelect = { newSex = it },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OpenCultTextField(
                        value = newHeight,
                        onValueChange = { newHeight = it },
                        label = "Height (cm)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OpenCultTextField(
                        value = newWindow,
                        onValueChange = { newWindow = it },
                        label = "Match Window (kg)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Primary Health Connect User", style = OpenCultTheme.typography.body, color = colors.textPrimary)
                    Switch(checked = newPrimaryHc, onCheckedChange = { newPrimaryHc = it })
                }
                OpenCultPrimaryButton(
                    text = "Save Member",
                    onClick = {
                        scope.launch {
                            viewModel.addMember(
                                displayName = newName,
                                sex = newSex,
                                heightCm = newHeight.toDoubleOrNull() ?: 170.0,
                                autoMatchWindowKg = newWindow.toDoubleOrNull() ?: 2.5,
                                isPrimaryHc = newPrimaryHc,
                            ).onSuccess {
                                statusMessage = "Saved household member."
                                newName = ""
                                showAddSheet = false
                            }.onFailure { err ->
                                statusMessage = err.message
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
