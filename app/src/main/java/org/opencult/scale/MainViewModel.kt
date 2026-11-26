package org.opencult.scale

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.opencult.scale.core.audio.OpenCultSoundEngine
import org.opencult.scale.core.ble.CultBleController
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.healthconnect.HealthConnectGateway
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SecureVault
import org.opencult.scale.core.sync.SupabaseSyncEngine
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vault: SecureVault,
    private val activeMemberSession: ActiveMemberSession,
    private val soundEngine: OpenCultSoundEngine,
    private val bleController: CultBleController,
    private val healthConnectGateway: HealthConnectGateway,
    private val syncEngine: SupabaseSyncEngine,
) : ViewModel() {

    private val _themeMode = MutableStateFlow(parseThemeMode(vault.getThemeModePreference()))
    val themeMode: StateFlow<OpenCultThemeMode> = _themeMode.asStateFlow()

    private val _isOnboarded = MutableStateFlow(vault.isOnboardingCompleted())
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _bleGranted = MutableStateFlow(checkBleGranted())
    val bleGranted: StateFlow<Boolean> = _bleGranted.asStateFlow()

    private val _hcGrantedCount = MutableStateFlow(0)
    val hcGrantedCount: StateFlow<Int> = _hcGrantedCount.asStateFlow()

    val appVersionLabel: String = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"

    init {
        soundEngine.isEnabled = activeMemberSession.soundFeedbackEnabled.value
        activeMemberSession.soundFeedbackEnabled
            .onEach { enabled -> soundEngine.isEnabled = enabled }
            .launchIn(viewModelScope)
        refreshRuntimePermissions()
        syncEngine.client
    }

    fun requestedHcPermissions(): Set<String> = HealthConnectGateway.requestedPermissions(
        includeRestingHr = activeMemberSession.restingHrOptInEnabled.value,
        includeBackgroundRead = activeMemberSession.backgroundHcReadEnabled.value,
    )

    fun refreshRuntimePermissions() {
        _bleGranted.value = checkBleGranted()
        bleController.refreshPermissionAndAdapterState()
        viewModelScope.launch {
            _hcGrantedCount.value = runCatching {
                healthConnectGateway.getGrantedPermissions().size
            }.getOrDefault(0)
        }
    }

    fun setThemeMode(mode: OpenCultThemeMode) {
        _themeMode.value = mode
        vault.setThemeModePreference(mode.name)
    }

    fun markOnboarded() {
        vault.setOnboardingCompleted(true)
        _isOnboarded.value = true
    }

    fun handleIncomingOAuthIntent(intent: Intent?) {
        syncEngine.handleIncomingIntent(intent)
    }

    private fun checkBleGranted(): Boolean {
        val scan = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
        val connect = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        return scan && connect
    }

    private fun parseThemeMode(raw: String): OpenCultThemeMode =
        runCatching { OpenCultThemeMode.valueOf(raw) }.getOrDefault(OpenCultThemeMode.DAYLIGHT)
}
