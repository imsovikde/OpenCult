package org.opencult.scale

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.opencult.scale.core.designsystem.components.DetachedObsidianDock
import org.opencult.scale.core.designsystem.components.OpenCultDestination
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
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
    data object WeighIn : OverlayRoute
    data object Inspector : OverlayRoute
    data class MeasurementDetail(val measurementId: String) : OverlayRoute
}

@Composable
fun OpenCultRootUi(
    isOnboarded: Boolean,
    themeMode: OpenCultThemeMode,
    bleGranted: Boolean,
    hcGrantedCount: Int,
    rationaleBanner: String? = null,
    appVersionLabel: String = "v0.1.0",
    onRequestBlePermissions: () -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    onChangeThemeMode: (OpenCultThemeMode) -> Unit,
    onOnboardingCompleted: () -> Unit,
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

    if (!isOnboarded) {
        OnboardingScreen(
            bleGranted = bleGranted,
            hcGrantedCount = hcGrantedCount,
            onRequestBlePermissions = onRequestBlePermissions,
            onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
            onCompleted = onOnboardingCompleted,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val currentOverlay = activeOverlay
        if (currentOverlay != null) {
            when (currentOverlay) {
                is OverlayRoute.WeighIn -> WeighInScreen(
                    onRequestBlePermissions = onRequestBlePermissions,
                    onClose = { activeOverlay = null },
                    onMeasurementCommitted = { id ->
                        activeOverlay = OverlayRoute.MeasurementDetail(id)
                    },
                )
                is OverlayRoute.Inspector -> InspectorScreen(
                    onBack = { activeOverlay = null },
                    onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
                )
                is OverlayRoute.MeasurementDetail -> MeasurementDetailScreen(
                    measurementId = currentOverlay.measurementId,
                    onBack = { activeOverlay = null },
                    onOpenInspector = { activeOverlay = OverlayRoute.Inspector },
                )
            }
        } else {
            Crossfade(
                targetState = activeTab,
                animationSpec = OpenCultMotion.scrimFade(reducedMotion),
                label = "root_tab_crossfade",
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

        if (activeOverlay == null) {
            DetachedObsidianDock(
                selectedDestination = activeTab,
                onSelectDestination = { activeTab = it },
                onMeasureClick = { activeOverlay = OverlayRoute.WeighIn },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (!rationaleBanner.isNullOrBlank()) {
            OpenCultBentoCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 48.dp),
                containerColor = OpenCultTheme.colors.cobaltSubtle,
            ) {
                Text(
                    text = rationaleBanner,
                    style = OpenCultTheme.typography.body,
                    color = OpenCultTheme.colors.textPrimary,
                )
            }
        }
    }
}
