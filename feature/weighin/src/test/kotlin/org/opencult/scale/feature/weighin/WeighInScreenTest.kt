package org.opencult.scale.feature.weighin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.bia.BiologicalSex
import org.opencult.scale.core.bia.BodyMetricsEngine
import org.opencult.scale.core.bia.ScaleTier
import org.opencult.scale.core.bia.UserAnthropometry
import org.opencult.scale.core.ble.ParsedScaleFrame
import org.opencult.scale.core.ble.ScaleFrameParser
import org.opencult.scale.core.ble.hexToByteArray
import org.opencult.scale.core.sync.ActiveMemberSession
import org.opencult.scale.core.sync.SecureVault
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeighInScreenTest {

    @Test
    fun member_lock_and_active_profile_persist_across_shared_active_member_session() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = SecureVault(context)
        val session = ActiveMemberSession(vault)

        session.selectActiveProfile("member-alex")
        session.setMemberLockEnabled(true)

        val reloaded = ActiveMemberSession(SecureVault(context))
        val uiContext = WeighInMemberContext(activeProfile = null, memberLockEnabled = reloaded.memberLockEnabled.value)
        assertThat(reloaded.activeProfileId.value).isEqualTo("member-alex")
        assertThat(uiContext.memberLockEnabled).isTrue()
        assertThat(ScaleTier.CULT_PRO_8E.sourceWireName).isEqualTo("ble_8e_handles")
        assertThat(ScaleTier.CULT_BF01_4E.sourceWireName).isEqualTo("ble_4e_feet")
    }

    @Test
    fun simulated_cs_bf01_and_pro_8e_vectors_compute_honest_active_metric_counts() {
        val csFrame = "CF 3C C0 4C 1D F4 01 00 00 00 97".hexToByteArray()
        val parsed = ScaleFrameParser.parse(csFrame, ScaleTier.CULT_BF01_4E, deviceName = "CS-BF01")
            as ParsedScaleFrame.WeightFrame
        val report4e = BodyMetricsEngine.calculate(
            UserAnthropometry(BiologicalSex.MALE, 30, 178.0),
            ScaleTier.CULT_BF01_4E,
            parsed.weightKg,
            parsed.heartRateBpm,
            singleFreq50KhzOhm = parsed.singleFreq50KhzOhm,
        )
        assertThat(report4e.metrics.count { it.value != null }).isAtLeast(35)
        assertThat(report4e.segmentalMuscle).isNull()
    }
}
