package com.example.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.data.repository.PreferencesManager
import com.example.ui.components.AvatarImage
import com.example.ui.theme.DarkBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    currentUser: UserProfile,
    preferencesManager: PreferencesManager,
    onProfileClick: () -> Unit,
    onThemesClick: () -> Unit,
    onVoiceEffectsClick: () -> Unit,
    onVideoEffectsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onQuickSwitchCalculator: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var readReceipts by remember { mutableStateOf(preferencesManager.readReceiptsEnabled) }
    var notificationPreview by remember { mutableStateOf(preferencesManager.notificationPreview) }
    var openCalcOnResume by remember { mutableStateOf(preferencesManager.openCalculatorOnResume) }
    var appLockEnabled by remember { mutableStateOf(preferencesManager.appLockEnabled) }
    var protectRecentApps by remember { mutableStateOf(preferencesManager.protectRecentApps) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B152B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        AvatarImage(photoUrl = currentUser.photoUrl, name = currentUser.displayName, size = 56.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.displayName,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Human ID: ${currentUser.humanId}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (currentUser.bio.isNotEmpty()) {
                                Text(
                                    text = currentUser.bio,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Edit Profile",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // PRIVACY & CHAT SECTION
            item {
                SettingsSection(title = "PRIVACY & CHAT") {
                    SettingsSwitchRow(
                        title = "Read Receipts",
                        subtitle = "Send and see blue double-ticks when messages are read",
                        icon = Icons.Default.Visibility,
                        checked = readReceipts,
                        onCheckedChange = {
                            readReceipts = it
                            preferencesManager.readReceiptsEnabled = it
                        }
                    )

                    SettingsSwitchRow(
                        title = "Message Previews",
                        subtitle = "Show message text and sender name in notifications",
                        icon = Icons.Default.Notifications,
                        checked = notificationPreview,
                        onCheckedChange = {
                            notificationPreview = it
                            preferencesManager.notificationPreview = it
                        }
                    )

                    SettingsSwitchRow(
                        title = "Open Calculator on Resume",
                        subtitle = "Quick privacy switch automatically lands on calculator",
                        icon = Icons.Default.Calculate,
                        checked = openCalcOnResume,
                        onCheckedChange = {
                            openCalcOnResume = it
                            preferencesManager.openCalculatorOnResume = it
                        }
                    )

                    SettingsNavigationRow(
                        title = "Chat Themes",
                        subtitle = "Current: ${preferencesManager.theme.replaceFirstChar { it.uppercase() }}",
                        icon = Icons.Default.Palette,
                        onClick = onThemesClick
                    )
                }
            }

            // SECURITY SECTION
            item {
                SettingsSection(title = "SECURITY & DISGUISE") {
                    SettingsSwitchRow(
                        title = "App Lock (4-Digit PIN)",
                        subtitle = if (appLockEnabled) "App Lock active" else "Require PIN to open Aether",
                        icon = Icons.Default.Lock,
                        checked = appLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinSetupDialog = true
                            } else {
                                appLockEnabled = false
                                preferencesManager.appLockEnabled = false
                                preferencesManager.appLockPin = ""
                            }
                        }
                    )

                    SettingsSwitchRow(
                        title = "Protect Recent App Previews",
                        subtitle = "Prevents screenshots and app switcher screen preview",
                        icon = Icons.Default.Security,
                        checked = protectRecentApps,
                        onCheckedChange = {
                            protectRecentApps = it
                            preferencesManager.protectRecentApps = it
                            Toast.makeText(context, "Restart app to fully apply screen protection", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // REAL-TIME AUDIO & VIDEO EFFECTS SECTION
            item {
                SettingsSection(title = "AUDIO & VIDEO EFFECTS") {
                    SettingsNavigationRow(
                        title = "Real-Time Voice Changer",
                        subtitle = "Preset: ${preferencesManager.voiceChangerPreset} (Test voice available)",
                        icon = Icons.Default.GraphicEq,
                        onClick = onVoiceEffectsClick
                    )

                    SettingsNavigationRow(
                        title = "Video Beauty Filters",
                        subtitle = "Preset: ${preferencesManager.videoBeautyPreset} (${(preferencesManager.videoBeautyIntensity * 100).toInt()}%)",
                        icon = Icons.Default.AutoFixHigh,
                        onClick = onVideoEffectsClick
                    )
                }
            }

            // UTILITIES & ADMIN
            item {
                SettingsSection(title = "ADMIN & SYSTEM") {
                    SettingsNavigationRow(
                        title = "JB • Admin Control Panel",
                        subtitle = "Server-authorized developer & moderation tools",
                        icon = Icons.Default.Security,
                        onClick = onAdminClick
                    )

                    SettingsNavigationRow(
                        title = "Quick Privacy Switch",
                        subtitle = "Open built-in standard & scientific calculator",
                        icon = Icons.Default.Calculate,
                        onClick = onQuickSwitchCalculator
                    )
                }
            }

            // Sign out button
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF241624)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log out",
                            tint = ErrorRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Log Out of Aether",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Aether v2.4.0 (Build 240)",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "WebRTC DTLS-SRTP • Firebase Cloud Infrastructure",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        Dialog(onDismissRequest = { showPinSetupDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B152B)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Set 4-Digit Lock PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter a 4-digit code to protect your chats",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2C2442),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newPinInput.length == 4) {
                                preferencesManager.appLockPin = newPinInput
                                preferencesManager.appLockEnabled = true
                                appLockEnabled = true
                                showPinSetupDialog = false
                                Toast.makeText(context, "App lock enabled", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable App Lock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161224)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}
