package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.components.FirebaseConfigSheet
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentSky
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateDarker
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    favoriteCount: Int,
    onUpdateProfile: (User) -> Unit,
    onAiInspectorClick: () -> Unit,
    onAiLeaseClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 95.dp)
    ) {
        // TOP PROFILE HEADER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user?.name?.firstOrNull() ?: 'S').toString().uppercase(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.name?.ifBlank { "StayFinder User" } ?: "StayFinder User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = user?.phone?.ifBlank { "Verified Phone Member" } ?: "Verified Phone Member",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                    if (!user?.collegeName.isNullOrBlank()) {
                        Text(
                            text = user.collegeName,
                            fontSize = 11.sp,
                            color = EmeraldLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.testTag("btn_edit_profile")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        tint = EmeraldLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STUDENT AI TOOLKIT
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Student AI Toolkit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                )
            ) {
                Column {
                    ProfileMenuRow(
                        title = "AI Room Inspector",
                        subtitle = "Scan room photo for lighting, ventilation & condition",
                        icon = Icons.Outlined.PhotoCamera,
                        iconTint = EmeraldPrimary,
                        onClick = onAiInspectorClick
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                    ProfileMenuRow(
                        title = "AI Lease Agreement Analyzer",
                        subtitle = "Check deposit deductions, curfew & lock-in terms",
                        icon = Icons.Outlined.Description,
                        iconTint = AccentSky,
                        onClick = onAiLeaseClick
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                    ProfileMenuRow(
                        title = "AI Accommodation Concierge",
                        subtitle = "Ask queries matched strictly against real properties",
                        icon = Icons.Filled.AutoAwesome,
                        iconTint = EmeraldLight,
                        onClick = onAiAssistantClick
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SYSTEM & SECURITY
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "System & Integration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                )
            ) {
                Column {
                    ProfileMenuRow(
                        title = "Firebase Setup & SHA-256 Keys",
                        subtitle = "View exact certificate fingerprints for native phone OTP",
                        icon = Icons.Filled.Security,
                        iconTint = EmeraldPrimary,
                        onClick = { showConfigSheet = true }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                    ProfileMenuRow(
                        title = "Saved Properties",
                        subtitle = "$favoriteCount properties bookmarked",
                        icon = Icons.Filled.Favorite,
                        iconTint = EmeraldLight,
                        onClick = { /* Displayed in home/explore */ }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                    ProfileMenuRow(
                        title = "Logout",
                        subtitle = "Sign out of your StayFinder account",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = AccentRose,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }
        }
    }

    // EDIT PROFILE DIALOG
    if (showEditDialog) {
        var nameInput by remember { mutableStateOf(user?.name ?: "") }
        var emailInput by remember { mutableStateOf(user?.email ?: "") }
        var collegeInput by remember { mutableStateOf(user?.collegeName ?: "") }
        var genderInput by remember { mutableStateOf(user?.gender ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Edit Student Profile", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = collegeInput,
                        onValueChange = { collegeInput = it },
                        label = { Text("College / University / Company") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = (user ?: User()).copy(
                            name = nameInput,
                            email = emailInput,
                            collegeName = collegeInput,
                            gender = genderInput
                        )
                        onUpdateProfile(updated)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
            containerColor = SlateCard
        )
    }

    // LOGOUT CONFIRMATION
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Sign Out?", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            },
            text = {
                Text("Are you sure you want to sign out of StayFinder?", color = SlateTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
            containerColor = SlateCard
        )
    }

    // CONFIG SHEET
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SlateCard
        ) {
            FirebaseConfigSheet(onDismiss = { showConfigSheet = false })
        }
    }
}

@Composable
private fun ProfileMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = SlateTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = SlateTextMuted,
            modifier = Modifier.size(13.dp)
        )
    }
}
