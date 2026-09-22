package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.ReadReceiptBlue
import com.example.ui.theme.SentGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.sin

@Composable
fun AvatarImage(
    photoUrl: String?,
    name: String,
    size: Dp = 48.dp,
    showOnlineBadge: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        if (!photoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // Elegant gradient initials fallback
            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
            val gradient = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(gradient)
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }

        if (showOnlineBadge && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.32f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun MessageStatusTicks(
    status: String,
    modifier: Modifier = Modifier
) {
    when (status) {
        "sending" -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                tint = SentGray,
                modifier = modifier.size(14.dp)
            )
        }
        "sent" -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = SentGray,
                modifier = modifier.size(15.dp)
            )
        }
        "delivered" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = SentGray,
                modifier = modifier.size(16.dp)
            )
        }
        "read" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = ReadReceiptBlue,
                modifier = modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AudioWaveformBubble(
    isPlaying: Boolean,
    progress: Float,
    durationMs: Long,
    onPlayPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Simulated sound wave bars
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 24
                val barWidth = 3.dp.toPx()
                val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)

                for (i in 0 until barCount) {
                    val x = i * (barWidth + spacing)
                    val factor = (sin(i * 0.75f) * 0.4f + 0.6f).coerceIn(0.2f, 1f)
                    val barHeight = size.height * factor
                    val top = (size.height - barHeight) / 2f
                    val isPastProgress = (i.toFloat() / barCount) <= progress

                    val barColor = if (isPastProgress) {
                        Color(0xFF8B5CF6)
                    } else {
                        Color(0xFF64748B)
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
fun ChatWallpaperContainer(
    wallpaper: String,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Base dark wallpaper
        when (wallpaper) {
            "midnight" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF090A14), Color(0xFF13152B), Color(0xFF06070E))
                            )
                        )
                )
            }
            "ocean" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF05101F), Color(0xFF0C243B), Color(0xFF030A14))
                            )
                        )
                )
            }
            "soft" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF160E18), Color(0xFF261628), Color(0xFF0E0810))
                            )
                        )
                )
            }
            "emerald" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF071712), Color(0xFF0E2A20), Color(0xFF040F0B))
                            )
                        )
                )
            }
            else -> {
                // Default dark with subtle subtle micro geometric pattern
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F0C1B))
                ) {
                    val step = 40.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        var y = 0f
                        while (y < size.height) {
                            drawCircle(
                                color = Color(0x0C8B5CF6),
                                radius = 1.5.dp.toPx(),
                                center = Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }
                }
            }
        }

        // Content on top
        content()
    }
}
