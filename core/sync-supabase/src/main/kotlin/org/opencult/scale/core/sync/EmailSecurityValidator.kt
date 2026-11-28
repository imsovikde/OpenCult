package org.opencult.scale.core.sync

import java.util.Locale

sealed interface EmailValidationResult {
    data class Valid(val normalizedEmail: String) : EmailValidationResult
    data class Invalid(val reason: String) : EmailValidationResult
}

sealed interface PasswordValidationResult {
    object Valid : PasswordValidationResult
    data class Invalid(val reason: String) : PasswordValidationResult
}

/**
 * Client-side defense-in-depth email & password validator mirroring server-side
 * GoTrue rules (`supabase/config.toml`) and `private.before_user_created_hook`.
 */
object EmailSecurityValidator {

    val DISPOSABLE_DOMAINS: Set<String> = setOf(
        "10minutemail.com",
        "10minutemail.net",
        "10minutemail.org",
        "20minutemail.com",
        "anonbox.net",
        "binkmail.com",
        "bobmail.info",
        "byom.de",
        "chammy.info",
        "cool.fr.nf",
        "courriel.fr.nf",
        "crazymailing.com",
        "devnullmail.com",
        "discard.email",
        "discardmail.com",
        "discardmail.de",
        "dispomail.eu",
        "disposableaddress.com",
        "disposableemailaddresses.com",
        "dispostable.com",
        "dodgeit.com",
        "dodgit.com",
        "drdrb.net",
        "dropmail.me",
        "duck2.club",
        "e4ward.com",
        "emailfake.com",
        "emailondeck.com",
        "emailtemporario.com.br",
        "emeil.in",
        "emeil.ir",
        "example.com",
        "example.net",
        "example.org",
        "fakemailgenerator.com",
        "fakeinbox.com",
        "fake-mail.cf",
        "fake-mail.ga",
        "fake-mail.ml",
        "filzmail.com",
        "ftml.net",
        "getairmail.com",
        "getnada.com",
        "gishpuppy.com",
        "gowikibooks.com",
        "grr.la",
        "guerrillamail.biz",
        "guerrillamail.com",
        "guerrillamail.de",
        "guerrillamail.info",
        "guerrillamail.net",
        "guerrillamail.org",
        "haltospam.com",
        "harakirimail.com",
        "hatespam.org",
        "hidemail.de",
        "ieh-mail.de",
        "imails.info",
        "inboxalias.com",
        "incognitomail.com",
        "incognitomail.org",
        "instant-mail.de",
        "ip6.li",
        "jetable.com",
        "jetable.fr.nf",
        "jetable.org",
        "junk1e.com",
        "keepmymail.com",
        "klzlk.com",
        "koszmail.pl",
        "kurzepost.de",
        "lhsdv.com",
        "lookugly.com",
        "lortemail.dk",
        "lr78.com",
        "luxusmail.org",
        "m4ilweb.info",
        "mail-temporaire.fr",
        "mail4trash.com",
        "mailbidon.com",
        "mailblocks.com",
        "mailbucket.org",
        "mailcatch.com",
        "maildrop.cc",
        "maildx.com",
        "maileater.com",
        "mailexpire.com",
        "mailforspam.com",
        "mailfreeonline.com",
        "mailin8r.com",
        "mailinator.com",
        "mailinator.net",
        "mailinator.org",
        "mailinator.us",
        "mailinator2.com",
        "mailme.lv",
        "mailmetrash.com",
        "mailmoat.com",
        "mailnesia.com",
        "mailnull.com",
        "mailscrap.com",
        "mailshell.com",
        "mailsiphon.com",
        "mailslite.com",
        "mailtemp.info",
        "mailzilla.com",
        "makemetheking.com",
        "mbx.cc",
        "mega.zik.dj",
        "meinspamschutz.de",
        "meltmail.com",
        "messagebeamer.de",
        "mierdamail.com",
        "mintemail.com",
        "moakt.com",
        "mobi.web.id",
        "moburl.com",
        "mohmal.com",
        "moncourrier.fr.nf",
        "monemail.fr.nf",
        "monmail.fr.nf",
        "mt2009.com",
        "mx0.wwwnew.eu",
        "mypartyclip.de",
        "myphantomemail.com",
        "mytemp.email",
        "nada.email",
        "nepwk.com",
        "nervmich.net",
        "nervtmich.net",
        "netzidiot.de",
        "no-spam.ws",
        "nobulk.com",
        "noclickemail.com",
        "nogmailspam.info",
        "nomail.xl.cx",
        "nomail2me.com",
        "nospam.ze.tc",
        "nospam4.us",
        "nospamfor.us",
        "nospamthanks.info",
        "notmailinator.com",
        "nowmymail.com",
        "nurfuerspam.de",
        "nwldx.com",
        "odaymail.com",
        "oneoffemail.com",
        "onewaymail.com",
        "online.ms",
        "oopi.org",
        "ordinaryamerican.net",
        "otherinbox.com",
        "ovpn.to",
        "owlpic.com",
        "pancakemail.com",
        "pjkh.com",
        "pjjkp.com",
        "plexolan.de",
        "poofy.org",
        "pookmail.com",
        "privacy.net",
        "proxymail.eu",
        "punkass.com",
        "put2.net",
        "putthisinyourspamdatabase.com",
        "quickinbox.com",
        "rcpt.at",
        "receiveee.com",
        "rmqkr.net",
        "rppkn.com",
        "safetymail.info",
        "sendspamhere.com",
        "sharklasers.com",
        "sofort-mail.de",
        "spamavert.com",
        "spambog.com",
        "spambog.de",
        "spambog.ru",
        "spambox.us",
        "spamcannon.com",
        "spamcannon.net",
        "spamcon.org",
        "spamcowboy.com",
        "spamcowboy.net",
        "spamday.com",
        "spamex.com",
        "spamfree24.org",
        "spamgourmet.com",
        "spamherelots.com",
        "spamhereplease.com",
        "spamhole.com",
        "spamify.com",
        "spaml.com",
        "spaml.de",
        "spammotel.com",
        "spamobox.com",
        "spamslicer.com",
        "spamspot.com",
        "spamthis.co.uk",
        "speed.1s.fr",
        "superrito.com",
        "suremail.info",
        "temp-mail.io",
        "temp-mail.org",
        "tempail.com",
        "tempmail.com",
        "tempmail.net",
        "tempr.email",
        "thankyou2010.com",
        "thisisnotmyrealemail.com",
        "throwawaymail.com",
        "tmailinator.com",
        "tmpmail.net",
        "tmpmail.org",
        "tradermail.info",
        "trash-mail.at",
        "trash-mail.com",
        "trash2009.com",
        "trashmail.at",
        "trashmail.com",
        "trashmail.me",
        "trashmail.net",
        "trashmail.org",
        "tyldd.com",
        "upliftnow.com",
        "venompen.com",
        "veryrealemail.com",
        "watchfull.net",
        "wegwerfmail.de",
        "wegwerfmail.net",
        "wegwerfmail.org",
        "wh4f.org",
        "willselfdestruct.com",
        "xagloo.com",
        "xemaps.com",
        "xents.com",
        "xmaily.com",
        "xoxy.net",
        "yep.it",
        "yogamaven.com",
        "yopmail.com",
        "yopmail.fr",
        "yopmail.net",
        "yuurok.com",
        "zehnminuten.de",
        "zippymail.info",
    )

    private val RESERVED_SUFFIXES = listOf(
        ".local",
        ".internal",
        ".invalid",
        ".test",
        ".example",
        ".localhost",
        ".arpa",
    )

    private val LOCAL_REGEX = Regex("^[A-Za-z0-9.!#\$%&'*+/=?^_`{|}~-]+$")
    private val DOMAIN_REGEX = Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")

    fun validateEmailForAuth(rawEmail: String): EmailValidationResult {
        val cleaned = rawEmail.trim().lowercase(Locale.US)
        if (cleaned.isEmpty()) {
            return EmailValidationResult.Invalid("Enter your email address.")
        }
        val atIdx = cleaned.indexOf('@')
        if (atIdx <= 0 || atIdx != cleaned.lastIndexOf('@')) {
            return EmailValidationResult.Invalid("Enter a valid email address with one @ symbol.")
        }
        val local = cleaned.substring(0, atIdx)
        val domain = cleaned.substring(atIdx + 1).trimEnd('.')
        if (local.length > 64 || !LOCAL_REGEX.matches(local) || local.startsWith('.') || local.endsWith('.') || local.contains("..")) {
            return EmailValidationResult.Invalid("Email username contains invalid characters.")
        }
        if (domain.length < 3 || domain.length > 253 || !DOMAIN_REGEX.matches(domain)) {
            return EmailValidationResult.Invalid("Enter a valid email domain (for example, name@gmail.com).")
        }
        if (domain == "localhost" || RESERVED_SUFFIXES.any { domain.endsWith(it) }) {
            return EmailValidationResult.Invalid("Use a real permanent email domain rather than a local or test address.")
        }
        val tld = domain.substringAfterLast('.')
        if (tld.length < 2 || tld.all { it.isDigit() }) {
            return EmailValidationResult.Invalid("Email top-level domain is invalid.")
        }
        if (isDisposableDomain(domain)) {
            return EmailValidationResult.Invalid(
                "Temporary or disposable email addresses are blocked. Please use a permanent email address.",
            )
        }
        return EmailValidationResult.Valid(cleaned)
    }

    fun isDisposableDomain(domain: String): Boolean {
        val parts = domain.trimEnd('.').lowercase(Locale.US).split('.')
        if (parts.size < 2) return false
        for (i in 0..parts.size - 2) {
            val suffix = parts.subList(i, parts.size).joinToString(".")
            if (suffix in DISPOSABLE_DOMAINS) return true
        }
        return false
    }

    fun validatePasswordForAuth(password: String): PasswordValidationResult {
        if (password.length < 8) {
            return PasswordValidationResult.Invalid("Password must be at least 8 characters long.")
        }
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLower || !hasUpper || !hasDigit) {
            return PasswordValidationResult.Invalid(
                "Use uppercase, lowercase, and at least one number in your password.",
            )
        }
        return PasswordValidationResult.Valid
    }

    fun friendlyAuthErrorMessage(throwable: Throwable): String {
        val raw = throwable.message.orEmpty()
        val lower = raw.lowercase(Locale.US)
        return when {
            "email not confirmed" in lower ->
                "Check your inbox and enter your confirmation code before signing in."
            "disposable" in lower || "temporary" in lower ->
                "Temporary or disposable email addresses are blocked. Use a permanent email address."
            "captcha" in lower || "turnstile" in lower ->
                "Bot verification expired or failed. Complete the security check again."
            "rate limit" in lower || "too many" in lower || "429" in lower ->
                "Too many attempts right now. Wait a few seconds and try again."
            "invalid login credentials" in lower ->
                "Incorrect email or password."
            "user already registered" in lower || "already been registered" in lower ->
                "An account with this email already exists. Switch to Sign In."
            raw.isNotBlank() -> raw.lineSequence().firstOrNull()?.take(180) ?: "Authentication failed."
            else -> "Authentication failed."
        }
    }
}
