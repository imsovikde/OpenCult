package org.opencult.scale.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch
import org.opencult.scale.core.bia.ActivityLevel
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.database.OpenCultRepository
import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultCalendarSheet
import org.opencult.scale.core.designsystem.components.OpenCultPillSelectorRow
import org.opencult.scale.core.designsystem.components.OpenCultPrimaryButton
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.components.OpenCultStatusChip
import org.opencult.scale.core.designsystem.components.OpenCultTextField
import org.opencult.scale.core.designsystem.components.PillOption
import org.opencult.scale.core.designsystem.components.openCultMicroPress
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.HealthConnectAvailability
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SecureVault
import org.opencult.scale.core.sync.SupabaseSyncEngine
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    private val vault: SecureVault,
    private val activeMemberSession: ActiveMemberSession,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
) : ViewModel() {

    fun healthConnectAvailability(): HealthConnectAvailability = healthConnectGateway.availability()

    suspend fun createInitialProfile(
        displayName: String,
        sex: BiologicalSex,
        birthDate: LocalDate,
        heightCm: Double,
        activityLevel: ActivityLevel,
        athleteMode: Boolean,
        targetWeightKg: Double?,
        preferredTier: ScaleTier,
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val profile = HouseholdProfileEntity(
            id = id,
            accountId = syncEngine.currentAccountIdOrGuest(),
            displayName = displayName.trim().ifBlank { "Primary Member" }.take(60),
            avatarColor = "#2185F7",
            biologicalSex = if (sex == BiologicalSex.FEMALE) "female" else "male",
            birthDate = birthDate.toString(),
            heightCm = heightCm.coerceIn(50.0, 260.0),
            activityLevel = activityLevel.name.lowercase(),
            athleteMode = athleteMode,
            autoMatchWindowKg = 2.50,
            targetWeightKg = targetWeightKg,
            targetBodyFatPct = if (sex == BiologicalSex.FEMALE) 22.0 else 15.0,
            isPrimaryHcUser = true,
            sortOrder = 0,
            createdAtMs = now,
            updatedAtMs = now,
        )
        repository.createOrUpdateProfile(profile)
        activeMemberSession.selectActiveProfile(id)
        vault.setOnboardingCompleted(true)
        syncEngine.enqueueOutboxDrain()
        return preferredTier.name
    }
}

@Composable
fun OnboardingScreen(
    bleGranted: Boolean,
    hcGrantedCount: Int,
    onRequestBlePermissions: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    OnboardingContent(
        bleGranted = bleGranted,
        hcGrantedCount = hcGrantedCount,
        onRequestBlePermissions = onRequestBlePermissions,
        onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
        onSaveProfile = { name, sex, dob, heightCm, activity, athlete, targetKg, tier ->
            scope.launch {
                viewModel.createInitialProfile(
                    displayName = name,
                    sex = sex,
                    birthDate = dob,
                    heightCm = heightCm,
                    activityLevel = activity,
                    athleteMode = athlete,
                    targetWeightKg = targetKg,
                    preferredTier = tier,
                )
                onCompleted()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun OnboardingContent(
    bleGranted: Boolean,
    hcGrantedCount: Int,
    onRequestBlePermissions: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    onSaveProfile: (
        displayName: String,
        sex: BiologicalSex,
        birthDate: LocalDate,
        heightCm: Double,
        activityLevel: ActivityLevel,
        athleteMode: Boolean,
        targetWeightKg: Double?,
        preferredTier: ScaleTier,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    val scroll = rememberScrollState()

    var displayName by remember { mutableStateOf("Souvik") }
    var sex by remember { mutableStateOf(BiologicalSex.MALE) }
    var birthDate by remember { mutableStateOf(LocalDate.of(1995, 5, 14)) }
    var showCalendar by remember { mutableStateOf(false) }
    var heightText by remember { mutableStateOf("176") }
    var targetWeightText by remember { mutableStateOf("72.0") }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var athleteMode by remember { mutableStateOf(false) }
    var selectedTier by remember { mutableStateOf(ScaleTier.CULT_PRO_8E) }
    var isSaving by remember { mutableStateOf(false) }

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
        // DRD:C-07 Sky-Pastel Welcome Wash + Miniature Phone Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(OpenCultTheme.shapes.heroSquircle)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.bentoSkySurface, colors.elevatedSurface),
                    ),
                )
                .border(1.dp, colors.borderSubtle, OpenCultTheme.shapes.heroSquircle)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OpenCultStatusChip(label = "Local-First")
                    Text(
                        text = "Own Every Metric.",
                        style = OpenCultTheme.typography.pageTitle,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Local Cult BLE capture, 56 body metrics, Health Connect sync, and optional encrypted backup.",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }

                // Miniature breakout phone card preview
                Column(
                    modifier = Modifier
                        .width(104.dp)
                        .clip(OpenCultTheme.shapes.bentoSquircle)
                        .background(colors.cardSurface)
                        .border(1.5.dp, colors.borderStrong, OpenCultTheme.shapes.bentoSquircle)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "74.8 kg",
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                        val heights = listOf(0.5f, 0.72f, 0.92f, 0.65f, 0.8f)
                        val barW = 8.dp.toPx()
                        val gap = 4.dp.toPx()
                        heights.forEachIndexed { idx, frac ->
                            val bh = size.height * frac
                            drawRoundRect(
                                color = if (idx == 2) colors.coralAlert else colors.heroSparkNeutral,
                                topLeft = Offset(idx * (barW + gap), size.height - bh),
                                size = Size(barW, bh),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .clip(OpenCultTheme.shapes.chipSquircle)
                                .background(colors.bentoLavenderSurface),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .clip(OpenCultTheme.shapes.chipSquircle)
                                .background(colors.bentoSageSurface),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(OpenCultTheme.shapes.pill)
                            .background(colors.dockBackground),
                    )
                }
            }
        }

        // Step 1: Hardware & Health Permissions with equal single-line height
        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "1. Hardware & Health Connect",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OpenCultStatusChip(
                    label = if (bleGranted) "BLE Granted" else "BLE Needed",
                    containerColor = if (bleGranted) colors.bentoSageSurface else colors.bentoPeachSurface,
                    contentColor = if (bleGranted) colors.bentoSageMuted else colors.bentoPeachFlame,
                )
                OpenCultStatusChip(
                    label = "HC: $hcGrantedCount perms",
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OpenCultSecondaryButton(
                    text = if (bleGranted) "BLE Ready" else "Bluetooth",
                    onClick = onRequestBlePermissions,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                OpenCultSecondaryButton(
                    text = "Health Connect",
                    onClick = onRequestHealthConnectPermissions,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        // Step 2: ONLY Two Physical Cult Scale SKUs
        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "2. Your Cult Scale Hardware",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose the scale model you own. Pro auto-detects feet-only sessions at weigh-in.",
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            OpenCultPillSelectorRow(
                options = persistentListOf(
                    PillOption(ScaleTier.CULT_PRO_8E, "Cult Smart Scale Pro (8E)"),
                    PillOption(ScaleTier.CULT_BF01_4E, "Cult Scale CS-BF01 (4E)"),
                ),
                selected = selectedTier,
                onSelect = { selectedTier = it },
            )
        }

        // Step 3: Profile Anthropometry with Custom Squircle Inputs
        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "3. Primary Member Anthropometry",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(12.dp))
            OpenCultTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display Name",
                placeholder = "e.g. Souvik",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OpenCultPillSelectorRow(
                options = persistentListOf(
                    PillOption(BiologicalSex.MALE, "Male"),
                    PillOption(BiologicalSex.FEMALE, "Female"),
                ),
                selected = sex,
                onSelect = { sex = it },
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OpenCultTheme.shapes.inputSquircle)
                    .background(colors.subtlePillSurface)
                    .openCultMicroPress(role = Role.Button) { showCalendar = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Birth Date", style = OpenCultTheme.typography.body, color = colors.textSecondary)
                OpenCultStatusChip(label = birthDate.toString())
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OpenCultTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = "Height (cm)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OpenCultTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    label = "Target (kg)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            OpenCultPillSelectorRow(
                options = persistentListOf(
                    PillOption(ActivityLevel.SEDENTARY, "Sedentary"),
                    PillOption(ActivityLevel.MILD, "Mild"),
                    PillOption(ActivityLevel.MODERATE, "Moderate"),
                    PillOption(ActivityLevel.HEAVY, "Heavy"),
                ),
                selected = activityLevel,
                onSelect = { activityLevel = it },
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Athlete Calibration", style = OpenCultTheme.typography.body, color = colors.textPrimary)
                Switch(checked = athleteMode, onCheckedChange = { athleteMode = it })
            }
        }

        OpenCultPrimaryButton(
            text = if (isSaving) "Saving..." else "Enter OpenCult Dashboard",
            enabled = !isSaving,
            onClick = {
                isSaving = true
                val h = heightText.toDoubleOrNull() ?: 176.0
                val tw = targetWeightText.toDoubleOrNull()
                onSaveProfile(
                    displayName,
                    sex,
                    birthDate,
                    h,
                    activityLevel,
                    athleteMode,
                    tw,
                    selectedTier,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showCalendar) {
        OpenCultCalendarSheet(
            selectedDate = birthDate,
            datesWithMeasurements = persistentSetOf(),
            onSelectDate = { birthDate = it },
            onDismiss = { showCalendar = false },
        )
    }
}

@Preview(name = "Onboarding Daylight 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun OnboardingDaylightPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        OnboardingContent(
            bleGranted = false,
            hcGrantedCount = 4,
            onRequestBlePermissions = {},
            onRequestHealthConnectPermissions = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
        )
    }
}

@Preview(name = "Onboarding Obsidian 1.5x", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun OnboardingObsidianPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
        OnboardingContent(
            bleGranted = true,
            hcGrantedCount = 14,
            onRequestBlePermissions = {},
            onRequestHealthConnectPermissions = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
        )
    }
}
