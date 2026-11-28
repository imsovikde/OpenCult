package org.opencult.scale.feature.onboarding

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.opencult.scale.core.designsystem.components.OpenCultBentoCard
import org.opencult.scale.core.designsystem.components.OpenCultSecondaryButton
import org.opencult.scale.core.designsystem.theme.OpenCultTheme
import org.opencult.scale.core.designsystem.theme.OpenCultThemeMode
import org.opencult.scale.core.sync.TurnstileWebBridge

@Composable
fun TurnstileChallengeDialog(
    siteKey: String,
    onTokenVerified: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var statusMessage by remember { mutableStateOf("Loading Cloudflare verification...") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        OpenCultBentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            containerColor = colors.elevatedSurface,
            borderColor = colors.borderStrong,
            shape = OpenCultTheme.shapes.sheetSquircle,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Human Verification",
                        style = OpenCultTheme.typography.sectionTitle,
                        color = colors.textPrimary,
                    )
                    OpenCultSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                    )
                }
                Text(
                    text = statusMessage,
                    style = OpenCultTheme.typography.body,
                    color = colors.textSecondary,
                )
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            TurnstileWebBridge.configureAndLoad(
                                webView = this,
                                siteKey = siteKey,
                                darkTheme = colors.isDark,
                                onToken = { tok ->
                                    mainHandler.post { onTokenVerified(tok) }
                                },
                                onError = { code ->
                                    mainHandler.post {
                                        statusMessage = "Verification failed ($code). Check internet connection and retry."
                                    }
                                },
                                onExpired = {
                                    mainHandler.post {
                                        statusMessage = "Check expired. Complete verification again."
                                    }
                                },
                            )
                        }
                    },
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.destroy()
                    },
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
