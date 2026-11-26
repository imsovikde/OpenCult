package org.opencult.scale.core.ble

import java.util.UUID

object CultBleConstants {
    val SERVICE_SCALE: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val CHAR_WRITE: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val CHAR_NOTIFY: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

    val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    const val SERVICE_SCALE_SHORT = "fff0"
    const val CHAR_WRITE_SHORT = "fff1"
    const val CHAR_NOTIFY_SHORT = "fff4"

    const val UNIT_KG = 0x00
    const val UNIT_LB = 0x01
    const val UNIT_ST = 0x02

    const val HANDSHAKE_SETTLE_DELAY_MS = 3000L
    const val DF_GRACE_DEADLINE_MS = 1200L
    const val EXPECTED_8E_DF_FRAMES = 4
    const val WEIGHT_LOCK_FRAMES = 3
    const val HR_LOCK_FRAMES = 3

    fun isSupportedDeviceName(name: String?): Boolean {
        val upper = name?.uppercase() ?: return false
        return upper.startsWith("CULT") ||
            upper.startsWith("CS-BF") ||
            upper.contains("BODYPOD") ||
            upper.startsWith("1BYONE") ||
            upper.contains("HEALTH SCALE") ||
            upper.startsWith("RUNSTAR")
    }

    fun isOneByoneOrBf01DeviceName(name: String?): Boolean {
        val upper = name?.uppercase() ?: return false
        return upper.startsWith("CS-BF") ||
            upper.startsWith("1BYONE") ||
            upper.contains("HEALTH SCALE")
    }

    /** Mode setup packet `FD 37 <unit> <group> 00..00 <xor>`, XOR computed dynamically across bytes 0..9. */
    fun buildModeSetupCommand(unitCode: Int = UNIT_KG, group: Int = 0x01): ByteArray {
        val payload = byteArrayOf(
            0xFD.toByte(),
            0x37.toByte(),
            (unitCode and 0xFF).toByte(),
            (group and 0xFF).toByte(),
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00,
        )
        var acc = 0
        for (i in 0 until payload.lastIndex) {
            acc = acc xor (payload[i].toInt() and 0xFF)
        }
        payload[payload.lastIndex] = (acc and 0xFF).toByte()
        return payload
    }

    fun buildModeSetupCommand(unitKg: Boolean): ByteArray =
        buildModeSetupCommand(unitCode = if (unitKg) UNIT_KG else UNIT_LB)

    /** History query command `F2 00`. */
    fun buildRequestHistoryCommand(): ByteArray = byteArrayOf(0xF2.toByte(), 0x00)

    /** History clear command `F2 01`. */
    fun buildClearHistoryCommand(): ByteArray = byteArrayOf(0xF2.toByte(), 0x01)

    /** RTC sync command `F1` carrying current calendar fields (`YYYY MM DD hh mm ss`). */
    fun buildRtcSyncCommand(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): ByteArray = byteArrayOf(
        0xF1.toByte(),
        ((year shr 8) and 0xFF).toByte(),
        (year and 0xFF).toByte(),
        (month and 0xFF).toByte(),
        (day and 0xFF).toByte(),
        (hour and 0xFF).toByte(),
        (minute and 0xFF).toByte(),
        (second and 0xFF).toByte(),
    )
}
