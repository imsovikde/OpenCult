package org.opencult.scale.core.ble

import java.util.UUID

object CultBleConstants {
    val SERVICE_SCALE: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val CHAR_WRITE: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val CHAR_NOTIFY: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

    val SERVICE_RUNSTAR_FFB0: UUID = UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb")
    val CHAR_RUNSTAR_WRITE: UUID = UUID.fromString("0000ffb1-0000-1000-8000-00805f9b34fb")
    val CHAR_RUNSTAR_LIVE: UUID = UUID.fromString("0000ffb2-0000-1000-8000-00805f9b34fb")
    val CHAR_RUNSTAR_RESULT: UUID = UUID.fromString("0000ffb3-0000-1000-8000-00805f9b34fb")

    val SERVICE_IC_1530: UUID = UUID.fromString("00001530-0000-1000-8000-00805f9b34fb")
    val SERVICE_BODY_COMPOSITION: UUID = UUID.fromString("0000181b-0000-1000-8000-00805f9b34fb")
    val SERVICE_WEIGHT_SCALE: UUID = UUID.fromString("0000181d-0000-1000-8000-00805f9b34fb")

    val SUPPORTED_SCALE_SERVICE_UUIDS: Set<UUID> = setOf(
        SERVICE_SCALE,
        SERVICE_RUNSTAR_FFB0,
        SERVICE_IC_1530,
        SERVICE_BODY_COMPOSITION,
        SERVICE_WEIGHT_SCALE,
    )

    val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    const val SERVICE_SCALE_SHORT = "fff0"
    const val CHAR_WRITE_SHORT = "fff1"
    const val CHAR_NOTIFY_SHORT = "fff4"
    const val SERVICE_RUNSTAR_SHORT = "ffb0"
    const val CHAR_RUNSTAR_WRITE_SHORT = "ffb1"
    const val CHAR_RUNSTAR_LIVE_SHORT = "ffb2"
    const val CHAR_RUNSTAR_RESULT_SHORT = "ffb3"

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
            upper.startsWith("RUNSTAR") ||
            upper.contains("FG2202") ||
            upper.contains("YUNMAI") ||
            upper.contains("FITDAYS") ||
            upper.contains("IFIT") ||
            upper.contains("SACOMA") ||
            upper.contains("LESCALE") ||
            upper.contains("HOFFEN")
    }

    fun isSupportedScaleService(serviceUuids: Collection<UUID>): Boolean =
        serviceUuids.any { it in SUPPORTED_SCALE_SERVICE_UUIDS }

    fun suggestedTierForDiscovery(
        name: String?,
        serviceUuids: Collection<UUID>,
    ): org.opencult.scale.core.bia.ScaleTier = when {
        isOneByoneOrBf01DeviceName(name) -> org.opencult.scale.core.bia.ScaleTier.CULT_BF01_4E
        SERVICE_RUNSTAR_FFB0 in serviceUuids && name.orEmpty().contains("FG2202", ignoreCase = true) ->
            org.opencult.scale.core.bia.ScaleTier.CULT_BF01_4E
        else -> org.opencult.scale.core.bia.ScaleTier.CULT_PRO_8E
    }

    fun isOneByoneOrBf01DeviceName(name: String?): Boolean {
        val upper = name?.uppercase() ?: return false
        return upper.startsWith("CS-BF") ||
            upper.startsWith("1BYONE") ||
            upper.contains("HEALTH SCALE")
    }

    /** Runstar / Icomon `0xFFB0` 20-byte outgoing ack frame (`0xB0` acknowledging `seq`). */
    fun buildRunstarAck(seqToAck: Int, outgoingSeq: Int = 0): ByteArray {
        val frame = ByteArray(20)
        frame[0] = (outgoingSeq and 0xFF).toByte()
        frame[1] = 0x03
        frame[2] = 0x00
        frame[3] = 0xB0.toByte()
        frame[4] = (seqToAck and 0xFF).toByte()
        frame[5] = 0x00
        frame[19] = runstarChecksum(frame).toByte()
        return frame
    }

    /** Runstar / Icomon `0xFFB0` `0xBA` user profile frame (adapted from openScale GPL-3.0 `RunstarR6Handler`). */
    fun buildRunstarUserInfoFrame(
        outgoingSeq: Int,
        epochSeconds: Long,
        heightCm: Int,
        ageYears: Int,
        isMale: Boolean,
    ): ByteArray {
        val frame = ByteArray(20)
        val sexAge = (if (isMale) 0x80 else 0x00) or ageYears.coerceIn(0, 0x7F)
        frame[0] = (outgoingSeq and 0xFF).toByte()
        frame[1] = 0x10
        frame[2] = 0x00
        frame[3] = 0xBA.toByte()
        frame[4] = (epochSeconds ushr 24).toByte()
        frame[5] = (epochSeconds ushr 16).toByte()
        frame[6] = (epochSeconds ushr 8).toByte()
        frame[7] = epochSeconds.toByte()
        frame[8] = 0x01
        frame[9] = 0x4A
        frame[14] = heightCm.coerceIn(50, 255).toByte()
        frame[17] = sexAge.toByte()
        frame[18] = 0x2F
        frame[19] = runstarChecksum(frame).toByte()
        return frame
    }

    fun runstarChecksum(frame: ByteArray): Int {
        if (frame.size < 20) return 0
        var sum = 0
        for (i in 3..18) {
            sum += frame[i].toInt() and 0xFF
        }
        return sum and 0x1F
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
