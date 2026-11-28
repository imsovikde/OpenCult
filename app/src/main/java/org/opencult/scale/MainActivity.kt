package org.opencult.scale

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.healthconnect.HealthConnectAvailability

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var incomingAction by mutableStateOf<String?>(null)
    private var permissionFeedback by mutableStateOf<String?>(null)
    private var blePermissionRequestedOnce = false

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        viewModel.refreshRuntimePermissions()
        val grantedAll = grants.isNotEmpty() && grants.values.all { it }
        if (grantedAll) {
            permissionFeedback = if (!viewModel.bluetoothEnabled.value) {
                "Bluetooth permission granted. Turn on Bluetooth to scan for your scale."
            } else {
                null
            }
        } else {
            val permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_SCAN,
            ) && !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            permissionFeedback = if (permanentlyDenied) {
                "Bluetooth permission is disabled. Tap Open Settings -> Permissions -> Nearby devices to enable it."
            } else {
                "Bluetooth scan and connect permissions are required to find nearby scales."
            }
        }
    }

    private val btEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshRuntimePermissions()
    }

    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
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
            val bluetoothEnabled by viewModel.bluetoothEnabled.collectAsStateWithLifecycle()
            val hcGrantedCount by viewModel.hcGrantedCount.collectAsStateWithLifecycle()
            val hcAvailability by viewModel.hcAvailability.collectAsStateWithLifecycle()
            val showHcRationale = incomingAction == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" ||
                incomingAction == Intent.ACTION_VIEW_PERMISSION_USAGE
            val rationaleTitle = getString(R.string.health_connect_rationale_title)
            val rationaleBody = getString(R.string.health_connect_rationale_body)

            OpenCultTheme(mode = themeMode) {
                OpenCultRootUi(
                    isOnboarded = isOnboarded,
                    themeMode = themeMode,
                    bleGranted = bleGranted,
                    bluetoothEnabled = bluetoothEnabled,
                    hcGrantedCount = hcGrantedCount,
                    hcAvailability = hcAvailability,
                    rationaleBanner = if (showHcRationale) "$rationaleTitle: $rationaleBody" else null,
                    permissionFeedback = permissionFeedback,
                    onDismissPermissionFeedback = { permissionFeedback = null },
                    appVersionLabel = viewModel.appVersionLabel,
                    onRequestBlePermissions = { requestBlePermissionsWithFallback() },
                    onEnableBluetooth = { requestEnableBluetooth() },
                    onOpenAppSettings = { openAppDetailsSettings() },
                    onRequestHealthConnectPermissions = { requestHealthConnectWithFallback() },
                    onOpenHealthConnectSettings = { openHealthConnectSettings() },
                    onChangeThemeMode = { viewModel.setThemeMode(it) },
                    onOnboardingCompleted = { viewModel.markOnboarded() },
                )
            }
        }
    }

    private fun requestBlePermissionsWithFallback() {
        val scanGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
        val connectGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED

        if (scanGranted && connectGranted) {
            if (!viewModel.bluetoothEnabled.value) {
                requestEnableBluetooth()
            } else {
                viewModel.refreshRuntimePermissions()
            }
            return
        }

        val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.BLUETOOTH_SCAN,
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.BLUETOOTH_CONNECT,
        )

        if (blePermissionRequestedOnce && !showRationale) {
            permissionFeedback = "Open App Settings -> Permissions -> Nearby devices to grant Bluetooth access."
            openAppDetailsSettings()
            return
        }

        blePermissionRequestedOnce = true
        blePermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            ),
        )
    }

    private fun requestEnableBluetooth() {
        val connectGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        if (!connectGranted) {
            requestBlePermissionsWithFallback()
            return
        }
        runCatching {
            btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }.onFailure {
            permissionFeedback = "Enable Bluetooth from your quick settings shade to scan for scales."
        }
    }

    private fun openAppDetailsSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            appSettingsLauncher.launch(intent)
        }
    }

    private fun requestHealthConnectWithFallback() {
        viewModel.refreshRuntimePermissions()
        when (viewModel.hcAvailability.value) {
            HealthConnectAvailability.AVAILABLE -> {
                runCatching {
                    hcPermissionLauncher.launch(viewModel.requestedHcPermissions())
                }.onFailure {
                    openHealthConnectSettings()
                }
            }
            HealthConnectAvailability.UPDATE_REQUIRED -> {
                permissionFeedback = "Health Connect needs an update before syncing records."
                val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding")
                    putExtra("overlay", true)
                    putExtra("callerId", packageName)
                }
                val fallbackWeb = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"),
                )
                runCatching { startActivity(marketIntent) }
                    .onFailure { runCatching { startActivity(fallbackWeb) } }
            }
            HealthConnectAvailability.UNAVAILABLE -> {
                permissionFeedback = "Health Connect is unavailable on this device. Your readings remain safely stored in OpenCult."
            }
        }
    }

    private fun openHealthConnectSettings() {
        runCatching {
            startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
        }.onFailure {
            permissionFeedback = "Open Android Settings -> Security & Privacy -> Health Connect to manage access."
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
