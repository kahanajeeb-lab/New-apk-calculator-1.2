package com.example.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.repository.PreferencesManager
import com.example.service.CallUiState
import com.example.service.VideoBeautyProcessor
import com.example.service.VoiceChangerEngine
import com.example.service.WebRtcCallManager
import com.example.ui.components.AvatarImage
import com.example.ui.theme.DarkBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CallScreen(
    callManager: WebRtcCallManager,
    callerName: String,
    callerPhoto: String?,
    callerHumanId: String,
    isVideo: Boolean,
    isIncoming: Boolean,
    preferencesManager: PreferencesManager,
    onCallEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by callManager.callState.collectAsState()
    val isMuted by callManager.isMuted.collectAsState()
    val isSpeakerOn by callManager.isSpeakerOn.collectAsState()
    val isCameraOn by callManager.isCameraOn.collectAsState()
    val durationSeconds by callManager.callDurationSeconds.collectAsState()

    var showVoiceEffectsDialog by remember { mutableStateOf(false) }
    var showVideoBeautyDialog by remember { mutableStateOf(false) }
    var selectedVoicePreset by remember { mutableStateOf(preferencesManager.voiceChangerPreset) }
    var selectedBeautyPreset by remember { mutableStateOf(preferencesManager.videoBeautyPreset) }
    var beautyIntensity by remember { mutableFloatStateOf(preferencesManager.videoBeautyIntensity) }

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    if (callState == CallUiState.ENDED) {
        onCallEnded()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Video or Audio Call Viewport
        if (isVideo && isCameraOn) {
            // Video Call View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF130F21), Color(0xFF0C0916))
                        )
                    )
            ) {
                // Remote video placeholder / stream
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AvatarImage(photoUrl = callerPhoto, name = callerName, size = 110.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = callerName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (callState == CallUiState.CONNECTED) formattedDuration else "Connecting video...",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Self PiP preview in top corner
                Box(
                    modifier = Modifier
                        .size(110.dp, 150.dp)
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF221A36))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You\n(${selectedBeautyPreset})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp
                    )
                }
            }
        } else {
            // Voice Call View
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    AvatarImage(photoUrl = callerPhoto, name = callerName, size = 100.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = callerName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ID: $callerHumanId",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (callState) {
                        CallUiState.RINGING_INCOMING -> "Incoming ${if (isVideo) "Video" else "Voice"} Call..."
                        CallUiState.RINGING_OUTGOING -> "Ringing..."
                        CallUiState.CONNECTED -> formattedDuration
                        else -> "Connecting..."
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (callState == CallUiState.CONNECTED) OnlineGreen else MaterialTheme.colorScheme.primary
                )

                if (selectedVoicePreset != "Clear") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Voice FX: $selectedVoicePreset active",
                        fontSize = 12.sp,
                        color = Color(0xFFA78BFA)
                    )
                }
            }
        }

        // Active Controls / Ringing Actions
        if (callState == CallUiState.RINGING_INCOMING && isIncoming) {
            // Incoming Call Buttons: Accept & Decline
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) {
                // Decline button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            callManager.endCurrentCall()
                            onCallEnded()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(ErrorRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decline", color = TextSecondary, fontSize = 13.sp)
                }

                // Accept button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            // Already triggered in dialog/call manager
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(OnlineGreen, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Accept", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            // Active / Outgoing In-Call Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp, start = 16.dp, end = 16.dp)
            ) {
                // Effects Row (Voice Changer & Video Beauty buttons)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    // Voice Changer Button
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF231B38)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { showVoiceEffectsDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice: $selectedVoicePreset",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isVideo) {
                        Spacer(modifier = Modifier.width(12.dp))
                        // Video Beauty Filter Button
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF231B38)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { showVideoBeautyDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = Color(0xFFF472B6),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Beauty: $selectedBeautyPreset",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Main Call Action Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mute Mic
                    IconButton(
                        onClick = { callManager.toggleMute() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(if (isMuted) Color(0xFF3B2430) else Color(0xFF1E1730), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) ErrorRed else TextPrimary
                        )
                    }

                    // Speaker
                    IconButton(
                        onClick = { callManager.toggleSpeaker() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(if (isSpeakerOn) MaterialTheme.colorScheme.primary else Color(0xFF1E1730), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) Color.White else TextPrimary
                        )
                    }

                    // If video: Camera toggle & switch
                    if (isVideo) {
                        IconButton(
                            onClick = { callManager.toggleCamera() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(if (!isCameraOn) Color(0xFF3B2430) else Color(0xFF1E1730), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Camera",
                                tint = if (!isCameraOn) ErrorRed else TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { callManager.switchCamera() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color(0xFF1E1730), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = TextPrimary
                            )
                        }
                    }

                    // End Call
                    IconButton(
                        onClick = {
                            callManager.endCurrentCall()
                            onCallEnded()
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .background(ErrorRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    // Voice Changer Selection Dialog during active call
    if (showVoiceEffectsDialog) {
        Dialog(onDismissRequest = { showVoiceEffectsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1429)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Real-Time Voice Changer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Applied instantly through the audio DSP pipeline",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    VoiceChangerEngine.presets.forEach { preset ->
                        val isSelected = selectedVoicePreset.equals(preset.name, ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    selectedVoicePreset = preset.name
                                    preferencesManager.voiceChangerPreset = preset.name
                                    showVoiceEffectsDialog = false
                                }
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = preset.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Video Beauty Filter Selection Dialog during active video call
    if (showVideoBeautyDialog) {
        Dialog(onDismissRequest = { showVideoBeautyDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1429)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Real-Time Beauty Filters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Skin smoothing with eye, contour & edge preservation",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(VideoBeautyProcessor.presets) { preset ->
                            val isSelected = selectedBeautyPreset.equals(preset.name, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF251C38))
                                    .clickable {
                                        selectedBeautyPreset = preset.name
                                        preferencesManager.videoBeautyPreset = preset.name
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Intensity: ${(beautyIntensity * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Slider(
                        value = beautyIntensity,
                        onValueChange = {
                            beautyIntensity = it
                            preferencesManager.videoBeautyIntensity = it
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { showVideoBeautyDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
