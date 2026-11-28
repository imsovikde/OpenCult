package org.opencult.scale.core.sync

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep

/**
 * Builds and binds the hardened Cloudflare Turnstile HTML page for Android WebView.
 * Enforces explicit origin (`https://auth.opencult.local/`) with file/content access disabled
 * and `@Keep`-protected `@JavascriptInterface` callbacks under R8 minification.
 */
object TurnstileWebBridge {

    const val BASE_ORIGIN = "https://auth.opencult.local/"
    const val JS_BRIDGE_NAME = "OpenCultTurnstile"

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
                      if (window.$JS_BRIDGE_NAME) window.$JS_BRIDGE_NAME.onToken(token);
                    },
                    'error-callback': function(code) {
                      if (window.$JS_BRIDGE_NAME) window.$JS_BRIDGE_NAME.onError(String(code || 'error'));
                    },
                    'expired-callback': function() {
                      if (window.$JS_BRIDGE_NAME) window.$JS_BRIDGE_NAME.onExpired();
                    }
                  });
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureAndLoad(
        webView: WebView,
        siteKey: String,
        darkTheme: Boolean,
        onToken: (String) -> Unit,
        onError: (String) -> Unit,
        onExpired: () -> Unit,
    ) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
        }
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri: Uri = request?.url ?: return true
                val host = uri.host.orEmpty()
                return !(uri.scheme == "https" && (host == "challenges.cloudflare.com" || host == "auth.opencult.local"))
            }
        }
        webView.addJavascriptInterface(Bridge(onToken, onError, onExpired), JS_BRIDGE_NAME)
        webView.loadDataWithBaseURL(
            BASE_ORIGIN,
            buildChallengeHtml(siteKey = siteKey, darkTheme = darkTheme),
            "text/html",
            "UTF-8",
            null,
        )
    }

    @Keep
    class Bridge(
        private val tokenCallback: (String) -> Unit,
        private val errorCallback: (String) -> Unit,
        private val expiredCallback: () -> Unit,
    ) {
        @JavascriptInterface
        fun onToken(token: String) {
            if (token.isNotBlank()) tokenCallback(token)
        }

        @JavascriptInterface
        fun onError(code: String) {
            errorCallback(code)
        }

        @JavascriptInterface
        fun onExpired() {
            expiredCallback()
        }
    }
}
