package org.opencult.scale

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.opencult.scale.core.designsystem.theme.OpenCultTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var incomingAction by mutableStateOf<String?>(null)

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshRuntimePermissions()
    }

    private val hcPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshRuntimePermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingAction = intent?.action
        viewModel.handleIncomingOAuthIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
            val bleGranted by viewModel.bleGranted.collectAsStateWithLifecycle()
            val hcGrantedCount by viewModel.hcGrantedCount.collectAsStateWithLifecycle()
            val showHcRationale = incomingAction == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" ||
                incomingAction == Intent.ACTION_VIEW_PERMISSION_USAGE
            val rationaleTitle = getString(R.string.health_connect_rationale_title)
            val rationaleBody = getString(R.string.health_connect_rationale_body)

            OpenCultTheme(mode = themeMode) {
                OpenCultRootUi(
                    isOnboarded = isOnboarded,
                    themeMode = themeMode,
                    bleGranted = bleGranted,
                    hcGrantedCount = hcGrantedCount,
                    rationaleBanner = if (showHcRationale) "$rationaleTitle: $rationaleBody" else null,
                    appVersionLabel = viewModel.appVersionLabel,
                    onRequestBlePermissions = {
                        blePermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                            ),
                        )
                    },
                    onRequestHealthConnectPermissions = {
                        runCatching {
                            hcPermissionLauncher.launch(viewModel.requestedHcPermissions())
                        }
                    },
                    onChangeThemeMode = { viewModel.setThemeMode(it) },
                    onOnboardingCompleted = { viewModel.markOnboarded() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingAction = intent.action
        viewModel.handleIncomingOAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRuntimePermissions()
    }
}
