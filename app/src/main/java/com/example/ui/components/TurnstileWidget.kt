package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.AccentRose
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

enum class TurnstileVerificationStatus {
    IDLE,
    CHALLENGING,
    TOKEN_RECEIVED,
    VALIDATING_BACKEND,
    VERIFIED,
    EXPIRED,
    FAILED
}

private class TurnstileJsBridge(
    private val onToken: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onExpired: () -> Unit
) {
    @JavascriptInterface
    fun onTokenReceived(token: String) {
        onToken(token)
    }

    @JavascriptInterface
    fun onError(error: String) {
        onError(error)
    }

    @JavascriptInterface
    fun onExpired() {
        onExpired()
    }
}

/**
 * Cloudflare Turnstile anti-bot security widget integrated inside StayFinder.
 * Presents a smooth M3 dark-themed card containing Cloudflare's official challenge script.
 * Never stores or exposes secret keys.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileWidget(
    status: TurnstileVerificationStatus,
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit,
    onExpired: () -> Unit,
    onResetRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    // Resolve public Turnstile site key
    val siteKey = remember {
        try {
            val key = BuildConfig::class.java.getField("TURNSTILE_SITEKEY").get(null) as? String
            if (!key.isNullOrBlank()) key.trim() else "1x00000000000000000000AA"
        } catch (_: Exception) {
            "1x00000000000000000000AA"
        }
    }

    val htmlContent = remember(siteKey, reloadTrigger) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
          <style>
            * { box-sizing: border-box; }
            html, body {
              margin: 0;
              padding: 0;
              background-color: transparent;
              color: #f1f5f9;
              display: flex;
              justify-content: center;
              align-items: center;
              min-height: 70px;
              overflow: hidden;
            }
            .turnstile-container {
              display: flex;
              justify-content: center;
              align-items: center;
              width: 100%;
            }
          </style>
        </head>
        <body>
          <div class="turnstile-container">
            <div class="cf-turnstile"
                 data-sitekey="$siteKey"
                 data-theme="dark"
                 data-size="flexible"
                 data-callback="onSuccess"
                 data-error-callback="onError"
                 data-expired-callback="onExpired">
            </div>
          </div>
          <script>
            function onSuccess(token) {
              if (window.AndroidBridge && window.AndroidBridge.onTokenReceived) {
                window.AndroidBridge.onTokenReceived(token);
              }
            }
            function onError(err) {
              if (window.AndroidBridge && window.AndroidBridge.onError) {
                window.AndroidBridge.onError(err || 'Turnstile verification failed');
              }
            }
            function onExpired() {
              if (window.AndroidBridge && window.AndroidBridge.onExpired) {
                window.AndroidBridge.onExpired();
              }
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    val borderColor = when (status) {
        TurnstileVerificationStatus.VERIFIED -> EmeraldPrimary
        TurnstileVerificationStatus.FAILED, TurnstileVerificationStatus.EXPIRED -> AccentRose
        TurnstileVerificationStatus.VALIDATING_BACKEND, TurnstileVerificationStatus.CHALLENGING -> EmeraldLight
        else -> SlateBorder
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag("turnstile_security_card"),
        shape = RoundedCornerShape(12.dp),
        color = SlateDark
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with Security Badge and Cloudflare Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (status == TurnstileVerificationStatus.VERIFIED) {
                            Icons.Filled.CheckCircle
                        } else if (status == TurnstileVerificationStatus.FAILED || status == TurnstileVerificationStatus.EXPIRED) {
                            Icons.Filled.ErrorOutline
                        } else {
                            Icons.Filled.Security
                        },
                        contentDescription = stringResource(R.string.turnstile_header_title),
                        tint = when (status) {
                            TurnstileVerificationStatus.VERIFIED -> EmeraldPrimary
                            TurnstileVerificationStatus.FAILED, TurnstileVerificationStatus.EXPIRED -> AccentRose
                            else -> EmeraldLight
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.turnstile_header_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextPrimary
                    )
                }

                // Cloudflare micro-branding
                Text(
                    text = "Cloudflare Turnstile",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: WebView or Verification Success Display
            if (status == TurnstileVerificationStatus.VERIFIED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.turnstile_status_verified),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldLight
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(AndroidColor.TRANSPARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true

                                val bridge = TurnstileJsBridge(
                                    onToken = { token ->
                                        post { onTokenReceived(token) }
                                    },
                                    onError = { err ->
                                        post { onError(err) }
                                    },
                                    onExpired = {
                                        post { onExpired() }
                                    }
                                )
                                addJavascriptInterface(bridge, "AndroidBridge")

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        Log.d("TurnstileWidget", "Turnstile WebView loaded")
                                    }
                                }

                                loadDataWithBaseURL(
                                    "https://challenges.cloudflare.com",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webViewInstance = this
                            }
                        },
                        update = { webView ->
                            // Update or reload when trigger changes
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Overlay spinner when validating with backend
                    if (status == TurnstileVerificationStatus.VALIDATING_BACKEND) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(SlateDark.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.turnstile_status_backend_validating),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Status message and retry button on error / expiration
            AnimatedVisibility(
                visible = status == TurnstileVerificationStatus.FAILED || status == TurnstileVerificationStatus.EXPIRED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (status == TurnstileVerificationStatus.EXPIRED) {
                            stringResource(R.string.turnstile_status_expired)
                        } else {
                            stringResource(R.string.turnstile_status_failed)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentRose,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            reloadTrigger += 1
                            onResetRequested()
                            webViewInstance?.loadDataWithBaseURL(
                                "https://challenges.cloudflare.com",
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.size(32.dp).testTag("turnstile_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.turnstile_retry),
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
