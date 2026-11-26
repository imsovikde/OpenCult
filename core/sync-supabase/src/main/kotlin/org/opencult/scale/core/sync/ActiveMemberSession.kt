package org.opencult.scale.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared reactive singleton broadcasting active household member selection, Member Lock state,
 * clinical sound feedback setting, and opt-in Health Connect read modes across all feature screens.
 */
@Singleton
class ActiveMemberSession @Inject constructor(
    private val vault: SecureVault,
) {
    private val _activeProfileId = MutableStateFlow(vault.getActiveProfileId())
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    private val _memberLockEnabled = MutableStateFlow(vault.isMemberLockEnabled())
    val memberLockEnabled: StateFlow<Boolean> = _memberLockEnabled.asStateFlow()

    private val _soundFeedbackEnabled = MutableStateFlow(vault.isSoundFeedbackEnabled())
    val soundFeedbackEnabled: StateFlow<Boolean> = _soundFeedbackEnabled.asStateFlow()

    private val _restingHrOptInEnabled = MutableStateFlow(vault.isRestingHrOptInEnabled())
    val restingHrOptInEnabled: StateFlow<Boolean> = _restingHrOptInEnabled.asStateFlow()

    private val _backgroundHcReadEnabled = MutableStateFlow(vault.isBackgroundHcReadEnabled())
    val backgroundHcReadEnabled: StateFlow<Boolean> = _backgroundHcReadEnabled.asStateFlow()

    fun selectActiveProfile(profileId: String?) {
        vault.setActiveProfileId(profileId)
        _activeProfileId.value = profileId
    }

    fun setMemberLockEnabled(enabled: Boolean) {
        vault.setMemberLockEnabled(enabled)
        _memberLockEnabled.value = enabled
    }

    fun setSoundFeedbackEnabled(enabled: Boolean) {
        vault.setSoundFeedbackEnabled(enabled)
        _soundFeedbackEnabled.value = enabled
    }

    fun setRestingHrOptInEnabled(enabled: Boolean) {
        vault.setRestingHrOptInEnabled(enabled)
        _restingHrOptInEnabled.value = enabled
    }

    fun setBackgroundHcReadEnabled(enabled: Boolean) {
        vault.setBackgroundHcReadEnabled(enabled)
        _backgroundHcReadEnabled.value = enabled
    }
}
