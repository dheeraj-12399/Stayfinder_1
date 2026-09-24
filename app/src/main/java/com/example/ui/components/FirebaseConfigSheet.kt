package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@Composable
fun FirebaseConfigSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firebaseManager = remember { FirebaseManager.getInstance(context) }
    val isInit = firebaseManager.isFirebaseInitialized
    val projectId = firebaseManager.getProjectId()
    val packageName = firebaseManager.getApplicationId()
    val sha1 = remember { firebaseManager.getCertificateFingerprint("SHA-1") }
    val sha256 = remember { firebaseManager.getCertificateFingerprint("SHA-256") }

    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Firebase & Native OTP Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isInit) EmeraldPrimary.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isInit) "Connected" else "Standby",
                        color = if (isInit) EmeraldLight else AccentAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To keep Phone Authentication 100% inside StayFinder and prevent Chrome / web browser reCAPTCHA redirects, register your Android App in Firebase Console with this Package Name and SHA-256 fingerprint.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Package Name
            ConfigFieldBox(
                label = "Android Package Name (applicationId)",
                value = packageName,
                onCopy = { copyToClipboard("Package Name", packageName) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Firebase Project ID
            ConfigFieldBox(
                label = "Connected Firebase Project ID",
                value = projectId,
                onCopy = { copyToClipboard("Project ID", projectId) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // SHA-256 Fingerprint
            ConfigFieldBox(
                label = "SHA-256 Fingerprint (Required for Native In-App OTP)",
                value = sha256,
                highlight = true,
                onCopy = { copyToClipboard("SHA-256", sha256) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // SHA-1 Fingerprint
            ConfigFieldBox(
                label = "SHA-1 Fingerprint (Standard)",
                value = sha1,
                onCopy = { copyToClipboard("SHA-1", sha1) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Step Guide Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "3-Step Setup in Firebase Console:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StepItem(
                        number = "1",
                        text = "Go to Firebase Console -> Project Settings -> Your Apps -> Android App."
                    )
                    StepItem(
                        number = "2",
                        text = "Paste the SHA-256 and SHA-1 fingerprints shown above under 'SHA certificate fingerprints'."
                    )
                    StepItem(
                        number = "3",
                        text = "Ensure 'Phone' provider is enabled in Firebase Console -> Authentication -> Sign-in method."
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cloudflare Turnstile Security Layer Information Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cloudflare Turnstile Verification:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SlateTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Verifies that the client is human before triggering SMS dispatch.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• Backend validates token with Cloudflare Siteverify using server-side secret.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• Firebase Phone Authentication sends real SMS OTP once security check passes.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ConfigFieldBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) EmeraldLight else SlateTextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (highlight) EmeraldPrimary.copy(alpha = 0.5f) else SlateBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onCopy)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = SlateTextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy",
                tint = if (highlight) EmeraldPrimary else SlateTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = EmeraldPrimary,
            modifier = Modifier.size(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = SlateTextSecondary,
            lineHeight = 16.sp
        )
    }
}
