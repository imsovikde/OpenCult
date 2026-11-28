package org.opencult.scale.core.sync

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri

sealed interface TurnstileNavigationDirective {
    data class Token(val token: String) : TurnstileNavigationDirective
    data class Error(val code: String) : TurnstileNavigationDirective
    object Expired : TurnstileNavigationDirective
    object AllowSubresource : TurnstileNavigationDirective
    object Block : TurnstileNavigationDirective
}

/**
 * Builds and binds the hardened Cloudflare Turnstile HTML page for Android WebView.
 * Enforces explicit origin (`https://auth.opencult.local/`) with file/content access disabled,
 * zero `@JavascriptInterface` reflection bridges, and allowlisted HTTPS navigation interception.
 */
object TurnstileWebBridge {

    const val BASE_ORIGIN = "https://auth.opencult.local/"
    const val BASE_HOST = "auth.opencult.local"
    const val CALLBACK_PATH = "/turnstile-callback"
    const val ERROR_PATH = "/turnstile-error"
    const val EXPIRED_PATH = "/turnstile-expired"

    fun buildChallengeHtml(siteKey: String, darkTheme: Boolean = false): String {
        val safeKey = siteKey.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        val theme = if (darkTheme) "dark" else "light"
        return """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <meta http-equiv="Content-Security-Policy"
                    content="default-src 'none'; script-src https://challenges.cloudflare.com 'unsafe-inline'; frame-src https://challenges.cloudflare.com; connect-src https://challenges.cloudflare.com; style-src 'unsafe-inline';" />
              <script src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit" async defer></script>
              <style>
                html, body {
                  margin: 0;
                  padding: 8px 0;
                  background: transparent;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  min-height: 72px;
                }
              </style>
            </head>
            <body>
              <div id="turnstile-container"></div>
              <script>
                (function initTurnstile() {
                  if (typeof turnstile === 'undefined') {
                    setTimeout(initTurnstile, 120);
                    return;
                  }
                  turnstile.render('#turnstile-container', {
                    sitekey: '$safeKey',
                    theme: '$theme',
                    callback: function(token) {
                      if (token) {
                        window.location.href = '${BASE_ORIGIN.trimEnd('/')}$CALLBACK_PATH?token=' + encodeURIComponent(token);
                      }
                    },
                    'error-callback': function(code) {
                      window.location.href = '${BASE_ORIGIN.trimEnd('/')}$ERROR_PATH?code=' + encodeURIComponent(String(code || 'error'));
                    },
                    'expired-callback': function() {
                      window.location.href = '${BASE_ORIGIN.trimEnd('/')}$EXPIRED_PATH';
                    }
                  });
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun parseNavigationDirective(uri: Uri): TurnstileNavigationDirective {
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return TurnstileNavigationDirective.Block
        }
        val host = uri.host.orEmpty().lowercase()
        return when (host) {
            BASE_HOST -> when (uri.path) {
                CALLBACK_PATH -> {
                    val tok = uri.getQueryParameter("token").orEmpty()
                    if (tok.isNotBlank()) TurnstileNavigationDirective.Token(tok) else TurnstileNavigationDirective.Block
                }
                ERROR_PATH -> TurnstileNavigationDirective.Error(
                    uri.getQueryParameter("code").orEmpty().ifBlank { "error" },
                )
                EXPIRED_PATH -> TurnstileNavigationDirective.Expired
                else -> TurnstileNavigationDirective.AllowSubresource
            }
            "challenges.cloudflare.com" -> TurnstileNavigationDirective.AllowSubresource
            else -> TurnstileNavigationDirective.Block
        }
    }

    fun parseNavigationUrl(rawUrl: String): TurnstileNavigationDirective =
        runCatching { parseNavigationDirective(rawUrl.toUri()) }.getOrDefault(TurnstileNavigationDirective.Block)

    @SuppressLint("SetJavaScriptEnabled")
    fun configureAndLoad(
        webView: WebView,
        siteKey: String,
        darkTheme: Boolean,
        onToken: (String) -> Unit,
        onError: (String) -> Unit,
        onExpired: () -> Unit,
    ) {
        val webSettings: WebSettings = webView.settings
        webSettings.setJavaScriptEnabled(true)
        webSettings.setDomStorageEnabled(true)
        webSettings.setAllowFileAccess(false)
        webSettings.setAllowContentAccess(false)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)
        webSettings.setSupportMultipleWindows(false)
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri: Uri = request?.url ?: return true
                return when (val directive = parseNavigationDirective(uri)) {
                    is TurnstileNavigationDirective.Token -> {
                        onToken(directive.token)
                        true
                    }
                    is TurnstileNavigationDirective.Error -> {
                        onError(directive.code)
                        true
                    }
                    TurnstileNavigationDirective.Expired -> {
                        onExpired()
                        true
                    }
                    TurnstileNavigationDirective.AllowSubresource -> false
                    TurnstileNavigationDirective.Block -> true
                }
            }
        }
        webView.loadDataWithBaseURL(
            BASE_ORIGIN,
            buildChallengeHtml(siteKey = siteKey, darkTheme = darkTheme),
            "text/html",
            "UTF-8",
            null,
        )
    }
}
