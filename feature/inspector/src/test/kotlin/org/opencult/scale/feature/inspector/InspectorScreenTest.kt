package org.opencult.scale.feature.inspector

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.opencult.scale.core.healthconnect.HealthConnectGateway

class InspectorScreenTest {

    @Test
    fun inspector_ui_state_and_opt_in_health_connect_permissions_behave_honestly() {
        val state = InspectorUiState()
        assertThat(state.restingHrOptIn).isFalse()
        assertThat(state.backgroundHcRead).isFalse()
        assertThat(InspectorTab.entries).hasSize(3)

        val base = HealthConnectGateway.requestedPermissions(includeRestingHr = false, includeBackgroundRead = false)
        val withRhr = HealthConnectGateway.requestedPermissions(includeRestingHr = true, includeBackgroundRead = false)
        val withAll = HealthConnectGateway.requestedPermissions(includeRestingHr = true, includeBackgroundRead = true)

        assertThat(withRhr.size).isEqualTo(base.size + 1)
        assertThat(withAll.size).isEqualTo(base.size + 2)
        assertThat(withAll).contains(HealthConnectGateway.OPT_IN_RESTING_HR_PERMISSION)
        assertThat(withAll).contains(HealthConnectGateway.OPT_IN_BACKGROUND_READ_PERMISSION)
    }
}
