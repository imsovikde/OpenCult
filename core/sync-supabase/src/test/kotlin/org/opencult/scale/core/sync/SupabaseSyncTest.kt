package org.opencult.scale.core.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencult.scale.core.database.OpenCultDatabase
import org.opencult.scale.core.database.OpenCultRepository
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupabaseSyncTest {

    private lateinit var ctx: Context
    private lateinit var db: OpenCultDatabase
    private lateinit var repo: OpenCultRepository
    private lateinit var vault: SecureVault
    private lateinit var engine: SupabaseSyncEngine

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, OpenCultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = OpenCultRepository(db)
        vault = SecureVault(ctx)
        engine = SupabaseSyncEngine(
            context = ctx,
            config = SupabaseConfig(url = "", anonKey = ""),
            vault = vault,
            db = db,
            repository = repo,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun oauth_redirect_sanitizer_accepts_only_opencult_auth_callback_and_rejects_foreign_hosts() {
        val good = Intent(Intent.ACTION_VIEW, Uri.parse("opencult://auth/callback#access_token=abc")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val badHost = Intent(Intent.ACTION_VIEW, Uri.parse("https://evil.example.com/auth/callback"))
        val badPath = Intent(Intent.ACTION_VIEW, Uri.parse("opencult://auth/other"))

        assertThat(OAuthRedirectSanitizer.sanitizeCallbackIntent(good)?.dataString)
            .isEqualTo("opencult://auth/callback#access_token=abc")
        assertThat(OAuthRedirectSanitizer.sanitizeCallbackIntent(badHost)).isNull()
        assertThat(OAuthRedirectSanitizer.sanitizeCallbackIntent(badPath)).isNull()
    }

    @Test
    fun secure_vault_encrypts_and_restores_sensitive_tokens() {
        vault.putEncryptedString("tok_key", "hc-token-value-123")
        assertThat(vault.getEncryptedString("tok_key")).isEqualTo("hc-token-value-123")
        vault.putEncryptedString("tok_key", null)
        assertThat(vault.getEncryptedString("tok_key")).isNull()
    }

    @Test
    fun realtime_ignores_self_echo_mutation_id_and_applies_remote_measurement() = runTest {
        val selfJson = Json.parseToJsonElement(
            """
            {
              "id": "m-101",
              "account_id": "acc-1",
              "profile_id": "p-1",
              "scale_device_id": null,
              "measured_at": "2026-11-26T08:30:00Z",
              "source": "ble_8e_handles",
              "assignment_state": "confirmed",
              "weight_kg": 74.8,
              "heart_rate_bpm": 68,
              "impedance_50khz_ohm": 540.0,
              "impedance_high_ohm": 520.35,
              "metrics_json": {"weight_kg": 74.8},
              "provenance_json": {"weight_kg": "DIRECT_HARDWARE"},
              "notes": null,
              "mutation_id": "mut-self-1",
              "created_at": "2026-11-26T08:30:00Z",
              "updated_at": "2026-11-26T08:30:00Z",
              "deleted_at": null
            }
            """.trimIndent(),
        ).jsonObject

        engine.recordSelfMutationId("mut-self-1")
        engine.applyRemoteRecordJson(selfJson)
        assertThat(db.measurementDao().getById("m-101")).isNull()

        val peerJson = Json.parseToJsonElement(
            selfJson.toString().replace("mut-self-1", "mut-peer-2"),
        ).jsonObject
        engine.applyRemoteRecordJson(peerJson)
        val stored = db.measurementDao().getById("m-101")
        assertThat(stored).isNotNull()
        assertThat(stored!!.weightKg).isWithin(0.01).of(74.8)
    }

    @Test
    fun active_member_session_broadcasts_reactively_and_persists_settings() {
        val session = ActiveMemberSession(vault)
        assertThat(session.activeProfileId.value).isNull()
        assertThat(session.memberLockEnabled.value).isFalse()
        assertThat(session.soundFeedbackEnabled.value).isTrue()

        session.selectActiveProfile("profile-42")
        session.setMemberLockEnabled(true)
        session.setSoundFeedbackEnabled(false)
        session.setRestingHrOptInEnabled(true)
        session.setBackgroundHcReadEnabled(true)

        val reloaded = ActiveMemberSession(vault)
        assertThat(reloaded.activeProfileId.value).isEqualTo("profile-42")
        assertThat(reloaded.memberLockEnabled.value).isTrue()
        assertThat(reloaded.soundFeedbackEnabled.value).isFalse()
        assertThat(reloaded.restingHrOptInEnabled.value).isTrue()
        assertThat(reloaded.backgroundHcReadEnabled.value).isTrue()
    }

    @Test
    fun email_validator_blocks_disposable_domains_subdomains_and_weak_passwords_while_allowing_real_mail() {
        assertThat(EmailSecurityValidator.validateEmailForAuth("bot@mailinator.com"))
            .isInstanceOf(EmailValidationResult.Invalid::class.java)
        assertThat(EmailSecurityValidator.validateEmailForAuth("spam@sub.yopmail.com"))
            .isInstanceOf(EmailValidationResult.Invalid::class.java)
        assertThat(EmailSecurityValidator.validateEmailForAuth("local@dev.local"))
            .isInstanceOf(EmailValidationResult.Invalid::class.java)
        assertThat(EmailSecurityValidator.validateEmailForAuth("fake@example.com"))
            .isInstanceOf(EmailValidationResult.Invalid::class.java)

        val valid = EmailSecurityValidator.validateEmailForAuth("Alice+OpenCult@Gmail.com")
        assertThat(valid).isEqualTo(EmailValidationResult.Valid("alice+opencult@gmail.com"))

        assertThat(EmailSecurityValidator.validatePasswordForAuth("weak12"))
            .isInstanceOf(PasswordValidationResult.Invalid::class.java)
        assertThat(EmailSecurityValidator.validatePasswordForAuth("onlylowercase1"))
            .isInstanceOf(PasswordValidationResult.Invalid::class.java)
        assertThat(EmailSecurityValidator.validatePasswordForAuth("OpenCult2026"))
            .isEqualTo(PasswordValidationResult.Valid)
    }

    @Test
    fun turnstile_web_bridge_escapes_site_key_and_includes_csp_header() {
        val html = TurnstileWebBridge.buildChallengeHtml("0x4AAA\"<script>alert(1)</script>", darkTheme = true)
        assertThat(html).contains("https://challenges.cloudflare.com/turnstile/v0/api.js")
        assertThat(html).contains("sitekey: '0x4AAAscriptalert1script'")
        assertThat(html).contains("theme: 'dark'")
        assertThat(html).contains("Content-Security-Policy")
    }

    @Test
    fun signup_and_otp_verify_fail_closed_for_disposable_email_and_forward_turnstile_token_via_mock_http() = runTest {
        val recordedBodies = mutableListOf<Pair<String, String>>()
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val bodyText = when (val content = request.body) {
                is TextContent -> content.text
                is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                else -> content.toString()
            }
            recordedBodies.add(path to bodyText)
            val headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            when {
                path.endsWith("/signup") -> respond(
                    content = """{"id":"11111111-1111-1111-1111-111111111111","aud":"authenticated","email":"alex@proton.me"}""",
                    status = HttpStatusCode.OK,
                    headers = headers,
                )
                path.endsWith("/verify") -> respond(
                    content = """
                        {
                          "access_token": "jwt-access-1",
                          "token_type": "bearer",
                          "expires_in": 3600,
                          "refresh_token": "jwt-refresh-1",
                          "user": {"id":"11111111-1111-1111-1111-111111111111","aud":"authenticated","email":"alex@proton.me"}
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headers,
                )
                path.endsWith("/resend") -> respond(
                    content = """{}""",
                    status = HttpStatusCode.OK,
                    headers = headers,
                )
                else -> respond(content = """{}""", status = HttpStatusCode.OK, headers = headers)
            }
        }

        val configuredEngine = SupabaseSyncEngine(
            context = ctx,
            config = SupabaseConfig(
                url = "https://mock.supabase.co",
                anonKey = "public-anon-key",
                turnstileSiteKey = "1x00000000000000000000AA",
            ),
            vault = vault,
            db = db,
            repository = repo,
        ).apply { testHttpEngine = mockEngine }

        // 1. Disposable email blocked locally before hitting network.
        val blocked = configuredEngine.signUpWithEmail("bot@mailinator.com", "OpenCult2026", "tok-1")
        assertThat(blocked.isFailure).isTrue()
        assertThat(recordedBodies).isEmpty()

        // 2. Missing Turnstile token blocked locally before hitting network.
        val missingCaptcha = configuredEngine.signUpWithEmail("alex@proton.me", "OpenCult2026", null)
        assertThat(missingCaptcha.isFailure).isTrue()
        assertThat(recordedBodies).isEmpty()

        // 3. Valid signup forwards captchaToken & enters confirmation wait state.
        val sent = configuredEngine.signUpWithEmail("alex@proton.me", "OpenCult2026", "cf-turnstile-token-99")
        assertThat(sent.isSuccess).isTrue()
        assertThat(sent.getOrThrow()).isEqualTo(AuthActionOutcome.VerificationEmailSent("alex@proton.me"))
        assertThat(recordedBodies.any { (p, b) -> p.endsWith("/signup") && b.contains("cf-turnstile-token-99") }).isTrue()

        // 4. OTP verification confirms account cleanly.
        val verified = configuredEngine.verifyEmailSignupOtp("alex@proton.me", "123456")
        assertThat(verified.isSuccess).isTrue()

        // 5. Resend confirmation passes captcha token too.
        val resent = configuredEngine.resendSignUpConfirmation("alex@proton.me", "cf-turnstile-token-100")
        assertThat(resent.isSuccess).isTrue()
    }
}
