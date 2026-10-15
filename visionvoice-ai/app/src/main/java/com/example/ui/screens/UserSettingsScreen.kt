package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.local.AiDetailLevel
import com.example.data.local.ContrastTheme
import com.example.ui.viewmodel.VisionViewModel
import kotlin.math.roundToInt

@Composable
fun UserSettingsScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Accessibility & Settings",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp
        )
        Text(
            text = "Customize speech output, high contrast themes, and AI key",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Gemini AI API Key Section
        SectionHeader(icon = Icons.Default.Key, title = "Gemini AI API Key")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var inputKey by remember(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
                val hasSystemKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                val hasActiveKey = settings.customApiKey.isNotBlank() || hasSystemKey

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "API Key Status",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = if (hasActiveKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (settings.customApiKey.isNotBlank()) "Custom Key Active" else if (hasSystemKey) "System Key Active" else "No Key Configured",
                            color = if (hasActiveKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Enter or paste your Gemini API key below to activate live AI vision and text reading:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("Paste key: AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }) {
                        Text("Get Free Key ↗", fontSize = 13.sp)
                    }

                    Row {
                        if (settings.customApiKey.isNotBlank()) {
                            TextButton(onClick = {
                                inputKey = ""
                                viewModel.updateCustomApiKey("")
                            }) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Button(
                            onClick = {
                                viewModel.updateCustomApiKey(inputKey)
                            }
                        ) {
                            Text("Save Key")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Speech Output Section
        SectionHeader(icon = Icons.Default.RecordVoiceOver, title = "Speech Synthesis")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Speaking Rate: ${(settings.speechRate * 100).roundToInt()}%",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (settings.speechRate < 0.9f) "Slow" else if (settings.speechRate > 1.3f) "Fast" else "Normal",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Slider(
                    value = settings.speechRate,
                    onValueChange = { viewModel.preferencesManager.updateSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Voice Pitch",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Slider(
                    value = settings.speechPitch,
                    onValueChange = { viewModel.preferencesManager.updateSpeechPitch(it) },
                    valueRange = 0.5f..1.8f,
                    steps = 12,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.speakCustomText("Drishti speech test. Voice rate and pitch configured successfully.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Spoken Voice")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // High Contrast Themes
        SectionHeader(icon = Icons.Default.Palette, title = "High Contrast Display")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                ThemeOptionRow(
                    title = "Dark Slate & Cyan (Default)",
                    subtitle = "Vibrant electric cyan on dark navy background",
                    isSelected = settings.contrastTheme == ContrastTheme.DARK_CYAN,
                    onClick = { viewModel.preferencesManager.updateContrastTheme(ContrastTheme.DARK_CYAN) }
                )
                ThemeOptionRow(
                    title = "Yellow on Pure Black",
                    subtitle = "High visibility taxi yellow on true black",
                    isSelected = settings.contrastTheme == ContrastTheme.YELLOW_ON_BLACK,
                    onClick = { viewModel.preferencesManager.updateContrastTheme(ContrastTheme.YELLOW_ON_BLACK) }
                )
                ThemeOptionRow(
                    title = "Amber on Pure Black",
                    subtitle = "Warm high-contrast amber on true black",
                    isSelected = settings.contrastTheme == ContrastTheme.AMBER_ON_BLACK,
                    onClick = { viewModel.preferencesManager.updateContrastTheme(ContrastTheme.AMBER_ON_BLACK) }
                )
                ThemeOptionRow(
                    title = "Light High Contrast",
                    subtitle = "Crisp deep navy on pure white",
                    isSelected = settings.contrastTheme == ContrastTheme.LIGHT_HIGH_CONTRAST,
                    onClick = { viewModel.preferencesManager.updateContrastTheme(ContrastTheme.LIGHT_HIGH_CONTRAST) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Guidance Detail Level
        SectionHeader(icon = Icons.Default.Psychology, title = "AI Description Detail")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                DetailLevelRow(
                    title = "Concise (Fast)",
                    description = "2-3 rapid sentences. Best when walking or in unfamiliar places.",
                    isSelected = settings.detailLevel == AiDetailLevel.CONCISE,
                    onClick = { viewModel.preferencesManager.updateDetailLevel(AiDetailLevel.CONCISE) }
                )
                DetailLevelRow(
                    title = "Balanced (Standard)",
                    description = "Key objects, spatial distances, and ground safety.",
                    isSelected = settings.detailLevel == AiDetailLevel.BALANCED,
                    onClick = { viewModel.preferencesManager.updateDetailLevel(AiDetailLevel.BALANCED) }
                )
                DetailLevelRow(
                    title = "Detailed (Comprehensive)",
                    description = "Full colors, text transcription, room layout and peripheral items.",
                    isSelected = settings.detailLevel == AiDetailLevel.DETAILED,
                    onClick = { viewModel.preferencesManager.updateDetailLevel(AiDetailLevel.DETAILED) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggles Section
        SectionHeader(icon = Icons.Default.Tune, title = "Feedback Preferences")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PreferenceToggleRow(
                    title = "Auto-Speak Results",
                    subtitle = "Automatically announce descriptions aloud after scan",
                    checked = settings.autoSpeakResults,
                    onCheckedChange = { viewModel.preferencesManager.updateAutoSpeak(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                PreferenceToggleRow(
                    title = "Audio Beep Cues",
                    subtitle = "Sound cue when scan starts and finishes",
                    checked = settings.audioCuesEnabled,
                    onCheckedChange = { viewModel.preferencesManager.updateAudioCues(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                PreferenceToggleRow(
                    title = "Haptic Vibration",
                    subtitle = "Distinct vibration patterns for buttons, success, and hazards",
                    checked = settings.hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.preferencesManager.updateHapticFeedback(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Drishti AI v1.0.0",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$title. $subtitle. ${if (isSelected) "Selected" else ""}"
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DetailLevelRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$title. $description. ${if (isSelected) "Selected" else ""}"
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
