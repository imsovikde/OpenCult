package org.opencult.scale

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.opencult.scale.core.designsystem.components.DetachedObsidianDock
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultDestination
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.HealthConnectAvailability
import org.opencult.scale.feature.bodymap.BodyMapScreen
import org.opencult.scale.feature.home.HomeScreen
import org.opencult.scale.feature.home.MeasurementDetailScreen
import org.opencult.scale.feature.inspector.InspectorScreen
import org.opencult.scale.feature.onboarding.OnboardingScreen
import org.opencult.scale.feature.profiles.ProfilesScreen
import org.opencult.scale.feature.reports.ReportsScreen
import org.opencult.scale.feature.weighin.WeighInScreen

data class OpenCultRouteNavInfo(val routeTag: String) : NavigationEventInfo()

sealed interface OverlayRoute {
    object WeighIn : OverlayRoute
    object Inspector : OverlayRoute
    data class MeasurementDetail(val measurementId: String) : OverlayRoute
}

@Composable
fun OpenCultRootUi(
    isOnboarded: Boolean,
    themeMode: OpenCultThemeMode,
    bleGranted: Boolean,
    hcGrantedCount: Int,
    rationaleBanner: String? = null,
    appVersionLabel: String = "v0.3.0",
    onRequestBlePermissions: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    onChangeThemeMode: (OpenCultThemeMode) -> Unit,
    onOnboardingCompleted: () -> Unit,
    bluetoothEnabled: Boolean = true,
    hcAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    permissionFeedback: String? = null,
    onDismissPermissionFeedback: () -> Unit = {},
    onEnableBluetooth: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenHealthConnectSettings: () -> Unit = {},
) {
    var activeTab by remember { mutableStateOf(OpenCultDestination.HOME) }
    var activeOverlay by remember { mutableStateOf<OverlayRoute?>(null) }
    val reducedMotion = OpenCultTheme.reducedMotion

    val backEnabled = activeOverlay != null || (isOnboarded && activeTab != OpenCultDestination.HOME)
    val navEventState = rememberNavigationEventState(
        currentInfo = OpenCultRouteNavInfo(
            when {
                !isOnboarded -> "onboarding"
                activeOverlay != null -> activeOverlay!!::class.simpleName ?: "overlay"
                else -> activeTab.route
            },
        ),
    )

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = backEnabled,
        onBackCompleted = {
            if (activeOverlay != null) {
                activeOverlay = null
            } else if (activeTab != OpenCultDestination.HOME) {
                activeTab = OpenCultDestination.HOME
            }
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isOnboarded,
            transitionSpec = { OpenCultMotion.onboardingRootTransition(reducedMotion) },
            label = "root_onboarding_switch",
        ) { onboarded ->
            if (!onboarded) {
                OnboardingScreen(
                    bleGranted = bleGranted,
                    bluetoothEnabled = bluetoothEnabled,
                    hcGrantedCount = hcGrantedCount,
                    hcAvailability = hcAvailability,
                    onRequestBlePermissions = onRequestBlePermissions,
                    onEnableBluetooth = onEnableBluetooth,
                    onOpenAppSettings = onOpenAppSettings,
                    onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
                    onOpenHealthConnectSettings = onOpenHealthConnectSettings,
                    onCompleted = onOnboardingCompleted,
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = activeOverlay,
                        transitionSpec = {
                            val enteringSheet = targetState is OverlayRoute.WeighIn
                            val exitingSheet = initialState is OverlayRoute.WeighIn && targetState == null
                            when {
                                enteringSheet -> OpenCultMotion.sheetOverlayTransition(entering = true, reducedMotion = reducedMotion)
                                exitingSheet -> OpenCultMotion.sheetOverlayTransition(entering = false, reducedMotion = reducedMotion)
                                targetState != null -> OpenCultMotion.detailOverlayTransition(entering = true, reducedMotion = reducedMotion)
                                else -> OpenCultMotion.detailOverlayTransition(entering = false, reducedMotion = reducedMotion)
                            }
                        },
                        label = "root_overlay_transition",
                    ) { overlay ->
                        if (overlay != null) {
                            when (overlay) {
                                OverlayRoute.WeighIn -> WeighInScreen(
                                    bleGranted = bleGranted,
                                    bluetoothEnabled = bluetoothEnabled,
                                    onRequestBlePermissions = onRequestBlePermissions,
                                    onEnableBluetooth = onEnableBluetooth,
                                    onOpenAppSettings = onOpenAppSettings,
                                    onClose = { activeOverlay = null },
                                    onMeasurementCommitted = { id ->
                                        activeOverlay = OverlayRoute.MeasurementDetail(id)
                                    },
                                )
                                OverlayRoute.Inspector -> InspectorScreen(
                                    onBack = { activeOverlay = null },
                                    onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
                                )
                                is OverlayRoute.MeasurementDetail -> MeasurementDetailScreen(
                                    measurementId = overlay.measurementId,
                                    onBack = { activeOverlay = null },
                                    onOpenInspector = { activeOverlay = OverlayRoute.Inspector },
                                )
                            }
                        } else {
                            AnimatedContent(
                                targetState = activeTab,
                                transitionSpec = {
                                    OpenCultMotion.tabContentTransition(
                                        forward = targetState.ordinal >= initialState.ordinal,
                                        reducedMotion = reducedMotion,
                                    )
                                },
                                label = "root_tab_transition",
                            ) { destination ->
                                when (destination) {
                                    OpenCultDestination.HOME -> HomeScreen(
                                        onOpenWeighIn = { activeOverlay = OverlayRoute.WeighIn },
                                        onOpenMeasurementDetail = { id -> activeOverlay = OverlayRoute.MeasurementDetail(id) },
                                        onOpenInspector = { activeOverlay = OverlayRoute.Inspector },
                                        onOpenProfiles = { activeTab = OpenCultDestination.PROFILES },
                                    )
                                    OpenCultDestination.BODY_MAP -> BodyMapScreen()
                                    OpenCultDestination.REPORTS -> ReportsScreen()
                                    OpenCultDestination.PROFILES -> ProfilesScreen(
                                        currentThemeMode = themeMode,
                                        onChangeThemeMode = onChangeThemeMode,
                                        onOpenInspector = { activeOverlay = OverlayRoute.Inspector },
                                        appVersionLabel = appVersionLabel,
                                    )
                                }
                            }
                        }
                    }

                    if (activeOverlay == null) {
                        DetachedObsidianDock(
                            selectedDestination = activeTab,
                            onSelectDestination = { activeTab = it },
                            onMeasureClick = { activeOverlay = OverlayRoute.WeighIn },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }

        val bannerText = permissionFeedback ?: rationaleBanner
        if (!bannerText.isNullOrBlank()) {
            OpenCultBentoCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 48.dp),
                containerColor = OpenCultTheme.colors.cobaltSubtle,
                borderColor = OpenCultTheme.colors.brandCobalt,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = bannerText,
                        style = OpenCultTheme.typography.body,
                        color = OpenCultTheme.colors.textPrimary,
                    )
                    if (permissionFeedback != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!bleGranted) {
                                OpenCultSecondaryButton(
                                    text = "App Settings",
                                    onClick = {
                                        onDismissPermissionFeedback()
                                        onOpenAppSettings()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            OpenCultSecondaryButton(
                                text = "Dismiss",
                                onClick = onDismissPermissionFeedback,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
