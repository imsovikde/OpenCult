package org.opencult.scale.core.ble

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler

/**
 * Compatibility [ContextWrapper] for `blessed-kotlin` 3.1.0 on Android 14+ (`targetSdk = 36`).
 *
 * `BluetoothCentralManager` and `BluetoothPeripheral` invoke two-argument `registerReceiver`
 * without `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED`, which throws [SecurityException]
 * on `targetSdk >= 34`. Returning `this` from [getApplicationContext] and appending
 * [Context.RECEIVER_EXPORTED] on API 33+ keeps all internal Bluetooth broadcast receivers
 * functional without modifying the prebuilt AAR.
 */
class BleReceiverCompatContext(base: Context) : ContextWrapper(base.applicationContext ?: base) {

    override fun getApplicationContext(): Context = this

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
    ): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        @Suppress("DEPRECATION")
        super.registerReceiver(receiver, filter)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        flags: Int,
    ): Intent? = super.registerReceiver(receiver, filter, ensureExportedFlagIfApi33Plus(flags))

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
    ): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        super.registerReceiver(
            receiver,
            filter,
            broadcastPermission,
            scheduler,
            Context.RECEIVER_EXPORTED,
        )
    } else {
        @Suppress("DEPRECATION")
        super.registerReceiver(receiver, filter, broadcastPermission, scheduler)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
        flags: Int,
    ): Intent? = super.registerReceiver(
        receiver,
        filter,
        broadcastPermission,
        scheduler,
        ensureExportedFlagIfApi33Plus(flags),
    )

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {
        if (receiver == null) return
        try {
            super.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already unregistered during GATT or adapter teardown.
        }
    }

    private fun ensureExportedFlagIfApi33Plus(flags: Int): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return flags
        val visibilityMask = Context.RECEIVER_EXPORTED or Context.RECEIVER_NOT_EXPORTED
        return if ((flags and visibilityMask) == 0) {
            flags or Context.RECEIVER_EXPORTED
        } else {
            flags
        }
    }
}
