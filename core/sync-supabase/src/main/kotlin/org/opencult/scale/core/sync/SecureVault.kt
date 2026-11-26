package org.opencult.scale.core.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware-backed Android Keystore AES-256-GCM vault storing sensitive Supabase session tokens,
 * Health Connect ChangesToken, theme mode, sound/member lock toggles, and active household profile (`INV-08`).
 */
@Singleton
class SecureVault @Inject constructor(
    @ApplicationContext context: Context,
) : SessionManager, CodeVerifierCache {

    private val prefs = context.getSharedPreferences("opencult_vault_store", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private fun getOrCreateAesKey(): SecretKey? = runCatching {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            gen.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            gen.generateKey()
        }
    }.getOrNull()

    fun putEncryptedString(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
            return
        }
        val secretKey = getOrCreateAesKey()
        val encoded = if (secretKey != null) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + ciphertext.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)
            "KS1:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } else {
            val raw = value.toByteArray(Charsets.UTF_8)
            "JVM:" + Base64.encodeToString(raw, Base64.NO_WRAP)
        }
        prefs.edit().putString(key, encoded).apply()
    }

    fun getEncryptedString(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return when {
            raw.startsWith("KS1:") -> runCatching {
                val payload = Base64.decode(raw.removePrefix("KS1:"), Base64.NO_WRAP)
                val ivLen = payload[0].toInt() and 0xFF
                val iv = payload.copyOfRange(1, 1 + ivLen)
                val ciphertext = payload.copyOfRange(1 + ivLen, payload.size)
                val secretKey = getOrCreateAesKey() ?: return null
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            }.getOrNull()
            raw.startsWith("JVM:") -> runCatching {
                String(Base64.decode(raw.removePrefix("JVM:"), Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull()
            else -> null
        }
    }

    fun getThemeModePreference(): String = prefs.getString(KEY_THEME_MODE, "DAYLIGHT") ?: "DAYLIGHT"
    fun setThemeModePreference(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getActiveProfileId(): String? = prefs.getString(KEY_ACTIVE_PROFILE, null)
    fun setActiveProfileId(profileId: String?) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply()
    }

    fun isMemberLockEnabled(): Boolean = prefs.getBoolean(KEY_MEMBER_LOCK, false)
    fun setMemberLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEMBER_LOCK, enabled).apply()
    }

    fun isSoundFeedbackEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)
    fun setSoundFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isRestingHrOptInEnabled(): Boolean = prefs.getBoolean(KEY_HC_RESTING_HR, false)
    fun setRestingHrOptInEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HC_RESTING_HR, enabled).apply()
    }

    fun isBackgroundHcReadEnabled(): Boolean = prefs.getBoolean(KEY_HC_BG_READ, false)
    fun setBackgroundHcReadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HC_BG_READ, enabled).apply()
    }

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)
    fun setOnboardingCompleted(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDED, done).apply()
    }

    fun getHealthConnectToken(): String? = getEncryptedString(KEY_HC_CHANGES_TOKEN)
    fun setHealthConnectToken(token: String?) {
        putEncryptedString(KEY_HC_CHANGES_TOKEN, token)
    }

    fun getLastSyncTimestampMs(): Long? =
        prefs.getLong(KEY_LAST_SYNC_MS, -1L).takeIf { it > 0L }

    fun setLastSyncTimestampMs(ts: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_MS, ts).apply()
    }

    override suspend fun saveSession(session: UserSession) {
        withContext(Dispatchers.IO) {
            putEncryptedString(KEY_SUPABASE_SESSION, json.encodeToString(UserSession.serializer(), session))
        }
    }

    override suspend fun loadSession(): UserSession = withContext(Dispatchers.IO) {
        val raw = getEncryptedString(KEY_SUPABASE_SESSION) ?: throw NoSessionFoundException()
        runCatching { json.decodeFromString(UserSession.serializer(), raw) }
            .getOrElse { throw NoSessionFoundException() }
    }

    override suspend fun deleteSession() {
        withContext(Dispatchers.IO) {
            putEncryptedString(KEY_SUPABASE_SESSION, null)
        }
    }

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        withContext(Dispatchers.IO) {
            putEncryptedString(KEY_PKCE_VERIFIER, codeVerifier)
        }
    }

    override suspend fun loadCodeVerifier(): String? = withContext(Dispatchers.IO) {
        getEncryptedString(KEY_PKCE_VERIFIER)
    }

    override suspend fun deleteCodeVerifier() {
        withContext(Dispatchers.IO) {
            putEncryptedString(KEY_PKCE_VERIFIER, null)
        }
    }

    fun generateLocalRandomTokenHex(byteLen: Int = 16): String {
        val bytes = ByteArray(byteLen)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val secureRandom = SecureRandom()
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "opencult_aes256_master"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SUPABASE_SESSION = "sb_session_json"
        private const val KEY_PKCE_VERIFIER = "sb_pkce_verifier"
        private const val KEY_THEME_MODE = "ui_theme_mode"
        private const val KEY_ACTIVE_PROFILE = "active_profile_id"
        private const val KEY_MEMBER_LOCK = "active_member_lock"
        private const val KEY_SOUND_ENABLED = "audio_sound_enabled"
        private const val KEY_HC_RESTING_HR = "hc_opt_in_resting_hr"
        private const val KEY_HC_BG_READ = "hc_opt_in_bg_read"
        private const val KEY_ONBOARDED = "onboarding_done"
        private const val KEY_HC_CHANGES_TOKEN = "hc_changes_token"
        private const val KEY_LAST_SYNC_MS = "last_sync_ms"
    }
}
