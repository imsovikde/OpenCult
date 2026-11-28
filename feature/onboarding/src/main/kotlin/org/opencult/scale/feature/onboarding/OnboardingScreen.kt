package org.opencult.scale.feature.onboarding

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
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
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.HealthConnectAvailability
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.AuthActionOutcome
import org.opencult.scale.core.sync.EmailSecurityValidator
import org.opencult.scale.core.sync.EmailValidationResult
import org.opencult.scale.core.sync.PasswordValidationResult
import org.opencult.scale.core.sync.SecureVault
import org.opencult.scale.core.sync.SupabaseSyncEngine
import org.opencult.scale.core.sync.SyncStatusSnapshot
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

private enum class OnboardingAuthMode {
    SIGN_UP,
    SIGN_IN,
}

@Immutable
data class OnboardingAuthState(
    val backendConfigured: Boolean = false,
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null,
    val turnstileRequired: Boolean = false,
    val turnstileSiteKey: String = "",
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OpenCultRepository,
    private val vault: SecureVault,
    private val activeMemberSession: ActiveMemberSession,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatusSnapshot> = syncEngine.status

    val turnstileRequired: Boolean
        get() = syncEngine.isTurnstileRequired

    val turnstileSiteKey: String
        get() = syncEngine.backendTurnstileSiteKey

    fun healthConnectAvailability(): HealthConnectAvailability = healthConnectGateway.availability()

    suspend fun signInWithEmail(
        email: String,
        password: String,
        captchaToken: String?,
    ): Result<Unit> = syncEngine.signInWithEmail(email, password, captchaToken)

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        captchaToken: String?,
    ): Result<AuthActionOutcome> = syncEngine.signUpWithEmail(email, password, captchaToken)

    suspend fun verifyEmailOtp(email: String, code: String): Result<Unit> =
        syncEngine.verifyEmailSignupOtp(email, code)

    suspend fun resendConfirmation(email: String, captchaToken: String?): Result<Unit> =
        syncEngine.resendSignUpConfirmation(email, captchaToken)

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
            displayName = displayName.trim().ifBlank { "Member" }.take(60),
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
    bluetoothEnabled: Boolean = true,
    hcAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    onEnableBluetooth: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenHealthConnectSettings: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    OnboardingContent(
        bleGranted = bleGranted,
        bluetoothEnabled = bluetoothEnabled,
        hcGrantedCount = hcGrantedCount,
        hcAvailability = hcAvailability,
        authState = OnboardingAuthState(
            backendConfigured = syncStatus.backendConfigured,
            isSignedIn = syncStatus.isSignedIn,
            signedInEmail = syncStatus.userEmail,
            turnstileRequired = viewModel.turnstileRequired,
            turnstileSiteKey = viewModel.turnstileSiteKey,
        ),
        onRequestBlePermissions = onRequestBlePermissions,
        onEnableBluetooth = onEnableBluetooth,
        onOpenAppSettings = onOpenAppSettings,
        onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
        onOpenHealthConnectSettings = onOpenHealthConnectSettings,
        onSignIn = { email, pass, tok -> viewModel.signInWithEmail(email, pass, tok) },
        onSignUp = { email, pass, tok -> viewModel.signUpWithEmail(email, pass, tok) },
        onVerifyOtp = { email, code -> viewModel.verifyEmailOtp(email, code) },
        onResendConfirmation = { email, tok -> viewModel.resendConfirmation(email, tok) },
        onOpenEmailApp = {
            runCatching {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        },
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
    bluetoothEnabled: Boolean = true,
    hcAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    authState: OnboardingAuthState = OnboardingAuthState(),
    onEnableBluetooth: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenHealthConnectSettings: () -> Unit = {},
    onSignIn: suspend (String, String, String?) -> Result<Unit> = { _, _, _ -> Result.success(Unit) },
    onSignUp: suspend (String, String, String?) -> Result<AuthActionOutcome> = { mail, _, _ ->
        Result.success(AuthActionOutcome.VerificationEmailSent(mail))
    },
    onVerifyOtp: suspend (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onResendConfirmation: suspend (String, String?) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onOpenEmailApp: () -> Unit = {},
    initialPage: Int = 0,
) {
    val colors = OpenCultTheme.colors
    val reducedMotion = OpenCultTheme.reducedMotion
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 3), pageCount = { 4 })

    fun goToPage(target: Int) {
        scope.launch {
            if (reducedMotion) {
                pagerState.scrollToPage(target.coerceIn(0, 3))
            } else {
                pagerState.animateScrollToPage(
                    page = target.coerceIn(0, 3),
                    animationSpec = OpenCultMotion.spatialDefault(false),
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
    ) {
        OnboardingStepHeader(
            currentPage = pagerState.currentPage,
            reducedMotion = reducedMotion,
            onBack = { goToPage(pagerState.currentPage - 1) },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = if (androidx.compose.ui.platform.LocalInspectionMode.current) 0 else 1,
        ) { page ->
            when (page) {
                0 -> OnboardingWelcomePage(
                    pagerState = pagerState,
                    reducedMotion = reducedMotion,
                    onGetStarted = { goToPage(1) },
                )
                1 -> OnboardingAccountSecurityPage(
                    authState = authState,
                    onSignIn = onSignIn,
                    onSignUp = onSignUp,
                    onVerifyOtp = onVerifyOtp,
                    onResendConfirmation = onResendConfirmation,
                    onOpenEmailApp = onOpenEmailApp,
                    onContinueNext = { goToPage(2) },
                )
                2 -> OnboardingPermissionsPage(
                    bleGranted = bleGranted,
                    bluetoothEnabled = bluetoothEnabled,
                    hcGrantedCount = hcGrantedCount,
                    hcAvailability = hcAvailability,
                    onRequestBlePermissions = onRequestBlePermissions,
                    onEnableBluetooth = onEnableBluetooth,
                    onOpenAppSettings = onOpenAppSettings,
                    onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
                    onOpenHealthConnectSettings = onOpenHealthConnectSettings,
                    onContinueNext = { goToPage(3) },
                )
                else -> OnboardingProfileSetupPage(
                    onSaveProfile = onSaveProfile,
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepHeader(
    currentPage: Int,
    reducedMotion: Boolean,
    onBack: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentPage > 0) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(OpenCultTheme.shapes.pill)
                    .background(colors.elevatedSurface)
                    .border(1.dp, colors.borderSubtle, OpenCultTheme.shapes.pill)
                    .openCultMicroPress(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Previous step" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Previous step",
                    tint = colors.textPrimary,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { idx ->
                val isActive = idx == currentPage
                val dotWidth by animateDpAsState(
                    targetValue = if (isActive) 26.dp else 8.dp,
                    animationSpec = OpenCultMotion.springDp(reducedMotion),
                    label = "step_dot_$idx",
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(OpenCultTheme.shapes.pill)
                        .background(if (isActive) colors.brandCobalt else colors.borderStrong),
                )
            }
        }

        Text(
            text = "${currentPage + 1} / 4",
            style = OpenCultTheme.typography.monoHex,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun OnboardingWelcomePage(
    pagerState: PagerState,
    reducedMotion: Boolean,
    onGetStarted: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .navigationBarsPadding()
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 10.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        // DRD:C-07 Sky-Pastel Welcome Wash + Miniature Phone Preview with GPU parallax
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
                        text = "Direct Cult BLE scale capture, 56 body metrics, two-way Health Connect sync, and optional cloud backup.",
                        style = OpenCultTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }

                Column(
                    modifier = Modifier
                        .width(102.dp)
                        .graphicsLayer {
                            if (!reducedMotion) {
                                translationX = -pagerState.currentPageOffsetFraction * 36f
                            }
                        }
                        .clip(OpenCultTheme.shapes.bentoSquircle)
                        .background(colors.cardSurface)
                        .border(1.5.dp, colors.borderStrong, OpenCultTheme.shapes.bentoSquircle)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "8E / 4E",
                        style = OpenCultTheme.typography.microLabel,
                        color = colors.brandCobalt,
                    )
                    Text(
                        text = "56 metrics",
                        style = OpenCultTheme.typography.numericInline,
                        color = colors.textPrimary,
                    )
                    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                        val heights = listOf(0.45f, 0.72f, 0.94f, 0.62f, 0.8f)
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
                                .height(16.dp)
                                .clip(OpenCultTheme.shapes.chipSquircle)
                                .background(colors.bentoLavenderSurface),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(OpenCultTheme.shapes.chipSquircle)
                                .background(colors.bentoSageSurface),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(OpenCultTheme.shapes.pill)
                            .background(colors.dockBackground),
                    )
                }
            }
        }

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Built For Your Scale Hardware",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Connects over Bluetooth LE without requiring GPS location permission. Raw frames stay visible in the Protocol Hub.",
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            OpenCultStatusChip(label = "GPL-3.0 Open Source Telemetry")
        }

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Two Honest Modes",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Use OpenCult completely offline on this phone, or pair a verified email account to sync across devices.",
                style = OpenCultTheme.typography.body,
                color = colors.textSecondary,
            )
        }

        OpenCultPrimaryButton(
            text = "Get Started",
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingAccountSecurityPage(
    authState: OnboardingAuthState,
    onSignIn: suspend (String, String, String?) -> Result<Unit>,
    onSignUp: suspend (String, String, String?) -> Result<AuthActionOutcome>,
    onVerifyOtp: suspend (String, String) -> Result<Unit>,
    onResendConfirmation: suspend (String, String?) -> Result<Unit>,
    onOpenEmailApp: () -> Unit,
    onContinueNext: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var mode by remember { mutableStateOf(OnboardingAuthMode.SIGN_UP) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var captchaToken by remember { mutableStateOf<String?>(null) }
    var showTurnstileDialog by remember { mutableStateOf(false) }
    var pendingOtpEmail by remember { mutableStateOf<String?>(null) }
    var otpCode by remember { mutableStateOf("") }
    var resendCooldownSec by remember { mutableIntStateOf(0) }
    var feedbackError by remember { mutableStateOf<String?>(null) }
    var feedbackInfo by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(resendCooldownSec) {
        if (resendCooldownSec > 0) {
            delay(1000L)
            resendCooldownSec--
        }
    }

    val liveEmailCheck = remember(email) { EmailSecurityValidator.validateEmailForAuth(email) }
    val emailError = when {
        !emailTouched && email.isBlank() -> null
        liveEmailCheck is EmailValidationResult.Invalid -> liveEmailCheck.reason
        else -> null
    }

    val livePasswordCheck = remember(password, mode) {
        if (mode == OnboardingAuthMode.SIGN_UP) {
            EmailSecurityValidator.validatePasswordForAuth(password)
        } else {
            if (password.isEmpty()) PasswordValidationResult.Invalid("Enter your password.") else PasswordValidationResult.Valid
        }
    }
    val passwordError = when {
        !passwordTouched && password.isEmpty() -> null
        livePasswordCheck is PasswordValidationResult.Invalid -> livePasswordCheck.reason
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .navigationBarsPadding()
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 10.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        Text(
            text = "Account & Sync",
            style = OpenCultTheme.typography.pageTitle,
            color = colors.textPrimary,
        )
        Text(
            text = "Sign in or verify a permanent email for encrypted cloud backup, or skip into Offline Local Mode.",
            style = OpenCultTheme.typography.body,
            color = colors.textSecondary,
        )

        if (authState.isSignedIn) {
            OpenCultBentoCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.bentoSageSurface,
                borderColor = colors.bentoSageAccent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = colors.bentoSageMuted)
                    Column {
                        Text(
                            text = "Email Verified & Signed In",
                            style = OpenCultTheme.typography.sectionTitle,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = authState.signedInEmail ?: "Authenticated Member",
                            style = OpenCultTheme.typography.body,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            OpenCultPrimaryButton(
                text = "Next: Hardware Permissions",
                onClick = onContinueNext,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Column
        }

        if (pendingOtpEmail != null) {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Confirm Your Email",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Enter the 6-digit verification code sent to $pendingOtpEmail, or tap the confirmation link in your inbox.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                OpenCultTextField(
                    value = otpCode,
                    onValueChange = { input ->
                        otpCode = input.filter { ch -> ch.isDigit() }.take(8)
                        feedbackError = null
                    },
                    label = "Verification Code",
                    placeholder = "123456",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    errorText = feedbackError,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OpenCultPrimaryButton(
                    text = if (busy) "Verifying..." else "Verify Code",
                    enabled = !busy && otpCode.length >= 6,
                    onClick = {
                        busy = true
                        feedbackError = null
                        scope.launch {
                            val res = onVerifyOtp(pendingOtpEmail!!, otpCode)
                            busy = false
                            res.onSuccess {
                                pendingOtpEmail = null
                                onContinueNext()
                            }.onFailure { err ->
                                feedbackError = err.message ?: "Verification code invalid or expired."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OpenCultSecondaryButton(
                        text = if (resendCooldownSec > 0) "Resend (${resendCooldownSec}s)" else "Resend Code",
                        enabled = !busy && resendCooldownSec == 0,
                        onClick = {
                            if (authState.turnstileRequired && captchaToken.isNullOrBlank()) {
                                showTurnstileDialog = true
                                return@OpenCultSecondaryButton
                            }
                            busy = true
                            scope.launch {
                                val res = onResendConfirmation(pendingOtpEmail!!, captchaToken)
                                busy = false
                                res.onSuccess {
                                    resendCooldownSec = 30
                                    feedbackInfo = "Confirmation email resent to $pendingOtpEmail."
                                }.onFailure { err ->
                                    feedbackError = err.message
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OpenCultSecondaryButton(
                        text = "Open Email App",
                        onClick = onOpenEmailApp,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OpenCultSecondaryButton(
                    text = "Use Different Email",
                    onClick = {
                        pendingOtpEmail = null
                        otpCode = ""
                        feedbackError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
                OpenCultPillSelectorRow(
                    options = persistentListOf(
                        PillOption(OnboardingAuthMode.SIGN_UP, "Create Account"),
                        PillOption(OnboardingAuthMode.SIGN_IN, "Sign In"),
                    ),
                    selected = mode,
                    onSelect = {
                        mode = it
                        feedbackError = null
                        feedbackInfo = null
                    },
                )
                Spacer(Modifier.height(12.dp))
                OpenCultTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailTouched = true
                        feedbackError = null
                    },
                    label = "Email Address",
                    placeholder = "name@gmail.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    errorText = emailError,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OpenCultTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordTouched = true
                        feedbackError = null
                    },
                    label = "Password",
                    placeholder = if (mode == OnboardingAuthMode.SIGN_UP) "8+ chars, upper, lower, number" else "Password",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    errorText = passwordError,
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .openCultMicroPress(role = Role.Button) {
                                    passwordVisible = !passwordVisible
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = colors.textSecondary,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (authState.turnstileRequired) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OpenCultTheme.shapes.inputSquircle)
                            .background(colors.subtlePillSurface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloudflare Turnstile",
                                style = OpenCultTheme.typography.buttonLabel,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (captchaToken != null) "Human check verified" else "Required before signup or sign in",
                                style = OpenCultTheme.typography.microLabel,
                                color = if (captchaToken != null) colors.bentoSageMuted else colors.textSecondary,
                            )
                        }
                        if (captchaToken != null) {
                            OpenCultStatusChip(
                                label = "Verified",
                                containerColor = colors.bentoSageSurface,
                                contentColor = colors.bentoSageMuted,
                            )
                        } else {
                            OpenCultSecondaryButton(
                                text = "Verify",
                                onClick = { showTurnstileDialog = true },
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = feedbackError != null || feedbackInfo != null) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        feedbackError?.let { err ->
                            Text(err, style = OpenCultTheme.typography.body, color = colors.coralAlert)
                        }
                        feedbackInfo?.let { info ->
                            Text(info, style = OpenCultTheme.typography.body, color = colors.brandCobalt)
                        }
                    }
                }

                if (!authState.backendConfigured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Cloud sync backend keys are not bundled in this build. You can verify email validation rules above or continue offline below.",
                        style = OpenCultTheme.typography.microLabel,
                        color = colors.textMuted,
                    )
                }

                Spacer(Modifier.height(12.dp))
                OpenCultPrimaryButton(
                    text = when {
                        busy -> "Please Wait..."
                        mode == OnboardingAuthMode.SIGN_UP -> "Create Verified Account"
                        else -> "Sign In"
                    },
                    enabled = !busy &&
                        authState.backendConfigured &&
                        liveEmailCheck is EmailValidationResult.Valid &&
                        livePasswordCheck is PasswordValidationResult.Valid,
                    onClick = {
                        emailTouched = true
                        passwordTouched = true
                        feedbackError = null
                        feedbackInfo = null
                        if (liveEmailCheck !is EmailValidationResult.Valid || livePasswordCheck !is PasswordValidationResult.Valid) {
                            return@OpenCultPrimaryButton
                        }
                        if (authState.turnstileRequired && captchaToken.isNullOrBlank()) {
                            showTurnstileDialog = true
                            return@OpenCultPrimaryButton
                        }
                        busy = true
                        scope.launch {
                            if (mode == OnboardingAuthMode.SIGN_UP) {
                                val res = onSignUp(email, password, captchaToken)
                                busy = false
                                captchaToken = null
                                res.onSuccess { outcome ->
                                    when (outcome) {
                                        is AuthActionOutcome.SignedIn -> onContinueNext()
                                        is AuthActionOutcome.VerificationEmailSent -> {
                                            pendingOtpEmail = outcome.email
                                            resendCooldownSec = 30
                                            feedbackInfo = "Verification email sent to ${outcome.email}."
                                        }
                                    }
                                }.onFailure { err ->
                                    feedbackError = err.message ?: "Account signup failed."
                                }
                            } else {
                                val res = onSignIn(email, password, captchaToken)
                                busy = false
                                captchaToken = null
                                res.onSuccess {
                                    onContinueNext()
                                }.onFailure { err ->
                                    feedbackError = err.message ?: "Sign in failed."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        OpenCultSecondaryButton(
            text = "Continue Offline (Local Storage Only)",
            onClick = onContinueNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showTurnstileDialog && authState.turnstileSiteKey.isNotBlank()) {
        TurnstileChallengeDialog(
            siteKey = authState.turnstileSiteKey,
            onTokenVerified = { token ->
                captchaToken = token
                showTurnstileDialog = false
                feedbackError = null
            },
            onDismiss = { showTurnstileDialog = false },
        )
    }
}

@Composable
private fun OnboardingPermissionsPage(
    bleGranted: Boolean,
    bluetoothEnabled: Boolean,
    hcGrantedCount: Int,
    hcAvailability: HealthConnectAvailability,
    onRequestBlePermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit,
    onContinueNext: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .navigationBarsPadding()
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 10.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        Text(
            text = "Hardware & Health",
            style = OpenCultTheme.typography.pageTitle,
            color = colors.textPrimary,
        )
        Text(
            text = "Allow Bluetooth to connect your Cult scale and Health Connect to sync compatible records.",
            style = OpenCultTheme.typography.body,
            color = colors.textSecondary,
        )

        val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.25f

        val bleCardContent: @Composable (Modifier) -> Unit = { cardModifier ->
            OpenCultBentoCard(modifier = cardModifier) {
                Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = colors.brandCobalt)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Bluetooth",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                OpenCultStatusChip(
                    label = when {
                        bleGranted && bluetoothEnabled -> "BLE Ready"
                        bleGranted && !bluetoothEnabled -> "BT Off"
                        else -> "Needed"
                    },
                    containerColor = if (bleGranted && bluetoothEnabled) colors.bentoSageSurface else colors.bentoPeachSurface,
                    contentColor = if (bleGranted && bluetoothEnabled) colors.bentoSageMuted else colors.bentoPeachFlame,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Scans scales without GPS location access.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.weight(1f, fill = !largeFont))
                Spacer(Modifier.height(10.dp))
                OpenCultSecondaryButton(
                    text = when {
                        bleGranted && !bluetoothEnabled -> "Turn On BT"
                        bleGranted -> "BLE Ready"
                        else -> "Allow Bluetooth"
                    },
                    onClick = {
                        if (bleGranted && !bluetoothEnabled) onEnableBluetooth() else onRequestBlePermissions()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val hcCardContent: @Composable (Modifier) -> Unit = { cardModifier ->
            OpenCultBentoCard(modifier = cardModifier) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = colors.coralAlert)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Health Connect",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                OpenCultStatusChip(
                    label = when (hcAvailability) {
                        HealthConnectAvailability.AVAILABLE -> "HC: $hcGrantedCount perms"
                        HealthConnectAvailability.UPDATE_REQUIRED -> "Update Req"
                        HealthConnectAvailability.UNAVAILABLE -> "Unavailable"
                    },
                    containerColor = if (hcGrantedCount > 0) colors.bentoSageSurface else colors.cobaltSubtle,
                    contentColor = if (hcGrantedCount > 0) colors.bentoSageMuted else colors.brandCobalt,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Optional 2-way Android Health sync.",
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.weight(1f, fill = !largeFont))
                Spacer(Modifier.height(10.dp))
                OpenCultSecondaryButton(
                    text = when {
                        hcAvailability == HealthConnectAvailability.UPDATE_REQUIRED -> "Update HC"
                        hcGrantedCount > 0 -> "HC Settings"
                        else -> "Allow Health"
                    },
                    onClick = {
                        if (hcGrantedCount > 0 && hcAvailability == HealthConnectAvailability.AVAILABLE) {
                            onOpenHealthConnectSettings()
                        } else {
                            onRequestHealthConnectPermissions()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (largeFont) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                bleCardContent(Modifier.fillMaxWidth())
                hcCardContent(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                bleCardContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                hcCardContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        if (!bleGranted) {
            OpenCultSecondaryButton(
                text = "Open App Settings (If Permission Blocked)",
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OpenCultBentoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.cobaltSubtle,
        ) {
            Text(
                text = "8 Health Connect Schemas vs Full Scale Dataset",
                style = OpenCultTheme.typography.buttonLabel,
                color = colors.brandCobalt,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Health Connect accepts 8 write record types (weight, body fat, lean mass, bone mass, body water, BMR, heart rate, height) and reads steps, sleep, and calories. OpenCult preserves all 20 CS-BF01 and 56 Cult Pro 8E metrics locally.",
                style = OpenCultTheme.typography.body,
                color = colors.textPrimary,
            )
        }

        OpenCultPrimaryButton(
            text = "Next: Scale & Profile",
            onClick = onContinueNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingProfileSetupPage(
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
) {
    val colors = OpenCultTheme.colors
    val scroll = rememberScrollState()

    var displayName by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(BiologicalSex.MALE) }
    var birthDate by remember { mutableStateOf(LocalDate.of(1995, 1, 1)) }
    var showCalendar by remember { mutableStateOf(false) }
    var heightText by remember { mutableStateOf("") }
    var targetWeightText by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var athleteMode by remember { mutableStateOf(false) }
    var selectedTier by remember { mutableStateOf(ScaleTier.CULT_PRO_8E) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .navigationBarsPadding()
            .padding(horizontal = OpenCultTheme.spacing.screenHorizontal, vertical = 10.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(OpenCultTheme.spacing.bentoGap),
    ) {
        Text(
            text = "Scale & Member Profile",
            style = OpenCultTheme.typography.pageTitle,
            color = colors.textPrimary,
        )

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Your Cult Scale Hardware",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose your model. Pro detects feet-only weigh-ins automatically when handles are not held.",
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

        OpenCultBentoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Primary Member Anthropometry",
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(12.dp))
            OpenCultTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    nameError = null
                },
                label = "Display Name",
                placeholder = "Your name",
                errorText = nameError,
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
                    onValueChange = {
                        heightText = it
                        heightError = null
                    },
                    label = "Height (cm)",
                    placeholder = "170",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    errorText = heightError,
                    modifier = Modifier.weight(1f),
                )
                OpenCultTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    label = "Target (kg)",
                    placeholder = "Optional",
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
                val trimmedName = displayName.trim()
                val parsedHeight = heightText.trim().toDoubleOrNull()
                if (trimmedName.isEmpty()) {
                    nameError = "Enter a member display name."
                }
                if (parsedHeight == null || parsedHeight !in 50.0..260.0) {
                    heightError = "Height must be between 50 and 260 cm."
                }
                if (trimmedName.isEmpty() || parsedHeight == null || parsedHeight !in 50.0..260.0) {
                    return@OpenCultPrimaryButton
                }
                isSaving = true
                val targetKg = targetWeightText.trim().toDoubleOrNull()?.takeIf { it in 20.0..300.0 }
                onSaveProfile(
                    trimmedName,
                    sex,
                    birthDate,
                    parsedHeight,
                    activityLevel,
                    athleteMode,
                    targetKg,
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

@Preview(name = "Onboarding Step 1 Daylight 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun OnboardingDaylightPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        OnboardingContent(
            bleGranted = false,
            hcGrantedCount = 4,
            onRequestBlePermissions = {},
            onRequestHealthConnectPermissions = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
            initialPage = 0,
        )
    }
}

@Preview(name = "Onboarding Step 3 Permissions 1.5x", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun OnboardingPermissionsObsidianPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.OBSIDIAN, reducedMotionOverride = true) {
        OnboardingContent(
            bleGranted = true,
            bluetoothEnabled = true,
            hcGrantedCount = 14,
            onRequestBlePermissions = {},
            onRequestHealthConnectPermissions = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
            initialPage = 2,
        )
    }
}

@Preview(name = "Onboarding Step 2 Account", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun OnboardingAccountPreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        OnboardingContent(
            bleGranted = false,
            hcGrantedCount = 0,
            authState = OnboardingAuthState(
                backendConfigured = true,
                turnstileRequired = true,
                turnstileSiteKey = "1x00000000000000000000AA",
            ),
            onRequestBlePermissions = {},
            onRequestHealthConnectPermissions = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
            initialPage = 1,
        )
    }
}

@Preview(name = "Onboarding Step 4 Profile", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun OnboardingProfilePreview() {
    OpenCultTheme(mode = OpenCultThemeMode.DAYLIGHT, reducedMotionOverride = true) {
        OnboardingContent(
            bleGranted = true,
            hcGrantedCount = 8,
            onRequestBlePermissions = {},
            onRequestHealthConnectPermissions = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
            initialPage = 3,
        )
    }
}
