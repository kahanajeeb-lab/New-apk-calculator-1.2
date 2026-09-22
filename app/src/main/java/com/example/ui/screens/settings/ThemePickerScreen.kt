package com.example.ui.screens.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PreferencesManager
import com.example.ui.theme.DarkBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val previewPrimary: Color,
    val previewSurface: Color
)

@Composable
fun ThemePickerScreen(
    preferencesManager: PreferencesManager,
    onThemeChanged: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTheme by remember { mutableStateOf(preferencesManager.theme) }

    val themes = listOf(
        ThemeOption(
            id = "default",
            name = "Obsidian Violet",
            description = "Default dark aesthetic with deep violet highlights",
            previewPrimary = Color(0xFF8B5CF6),
            previewSurface = Color(0xFF181428)
        ),
        ThemeOption(
            id = "purple",
            name = "Neon Purple",
            description = "Pure pitch black background with electric purple accents",
            previewPrimary = Color(0xFFA855F7),
            previewSurface = Color(0xFF150F2B)
        ),
        ThemeOption(
            id = "ocean",
            name = "Deep Ocean",
            description = "Abyssal navy background with bright cyan highlights",
            previewPrimary = Color(0xFF06B6D4),
            previewSurface = Color(0xFF0C1929)
        ),
        ThemeOption(
            id = "midnight",
            name = "Royal Midnight",
            description = "Smoky midnight navy with vibrant indigo accents",
            previewPrimary = Color(0xFF6366F1),
            previewSurface = Color(0xFF131524)
        ),
        ThemeOption(
            id = "soft",
            name = "Soft Rose",
            description = "Warm plum surface tones with delicate pink peach tints",
            previewPrimary = Color(0xFFF472B6),
            previewSurface = Color(0xFF251B27)
        ),
        ThemeOption(
            id = "classic",
            name = "Emerald Classic",
            description = "Timeless dark titanium with emerald green accents",
            previewPrimary = Color(0xFF10B981),
            previewSurface = Color(0xFF1A1A1A)
        ),
        ThemeOption(
            id = "gunmetal",
            name = "Tactical Gunmetal",
            description = "Slate carbon styling with cool metallic silver highlights",
            previewPrimary = Color(0xFF94A3B8),
            previewSurface = Color(0xFF1A202C)
        ),
        ThemeOption(
            id = "lavender",
            name = "Pastel Lavender",
            description = "Gentle lilac purple hues with subtle orchid vibrancy",
            previewPrimary = Color(0xFFC084FC),
            previewSurface = Color(0xFF211933)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Chat Themes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "CHOOSE YOUR AESTHETIC",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(themes) { item ->
                val isSelected = selectedTheme.equals(item.id, ignoreCase = true)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF231B38) else Color(0xFF161224)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTheme = item.id
                            preferencesManager.theme = item.id
                            onThemeChanged(item.id)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Color swatch preview
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(item.previewSurface)
                                .border(2.dp, item.previewPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(item.previewPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
