package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraPreviewContainer
import com.example.data.local.VisionMode
import com.example.ui.components.AudioControlBar
import com.example.ui.components.DetectedObjectsPills
import com.example.ui.components.HazardAlertBanner
import com.example.ui.components.LargeScanButton
import com.example.ui.components.ModeChip
import com.example.ui.components.VoiceCommandButton
import com.example.ui.viewmodel.VisionViewModel

@Composable
fun VisionHomeScreen(
    viewModel: VisionViewModel,
    hasCameraPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentMode by viewModel.currentMode.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val isListening by viewModel.voiceCommandManager.isListening.collectAsState()
    val askDialogVisible by viewModel.askDialogVisible.collectAsState()

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.ttsManager.speak("Photo imported. Analyzing image...")
                    viewModel.processCapturedBitmap(bitmap)
                }
            } catch (e: Exception) {
                viewModel.ttsManager.speak("Failed to load selected image.")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Viewfinder Preview with Tap-Anywhere gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            viewModel.repeatSpeech()
                        },
                        onTap = {
                            if (!isAnalyzing) {
                                viewModel.captureAndAnalyze()
                            }
                        }
                    )
                }
                .semantics {
                    contentDescription = "Camera viewfinder. Tap once to scan and analyze surroundings. Double tap to repeat audio."
                }
        ) {
            if (hasCameraPermission) {
                CameraPreviewContainer(
                    modifier = Modifier.fillMaxSize(),
                    onControllerReady = { controller ->
                        viewModel.cameraController = controller
                    }
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF090D14))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Drishti needs camera access to describe surroundings, detect obstacles, and read printed text.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRequestPermission) {
                            Text("Enable Camera Access")
                        }
                    }
                }
            }
        }

        // Top Control Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Drishti",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "AI Vision Guide",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flashlight toggle
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_toggle_torch")
                            .semantics {
                                contentDescription = if (isTorchOn) "Turn off flashlight" else "Turn on flashlight"
                            }
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = null,
                            tint = if (isTorchOn) MaterialTheme.colorScheme.tertiary else Color.White
                        )
                    }

                    // Camera Switch
                    IconButton(
                        onClick = { viewModel.switchCamera(lifecycleOwner) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_switch_camera")
                            .semantics { contentDescription = "Switch between front and back camera" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    // Gallery photo picker
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("action_pick_photo")
                            .semantics { contentDescription = "Import photo from gallery to analyze" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accessible Mode Selector Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                ModeChip(
                    title = "Scene",
                    isSelected = currentMode == VisionMode.SCENE,
                    icon = Icons.Default.Visibility,
                    onClick = { viewModel.switchMode(VisionMode.SCENE) }
                )
                ModeChip(
                    title = "Read Text",
                    isSelected = currentMode == VisionMode.OCR,
                    icon = Icons.Default.TextFields,
                    onClick = { viewModel.switchMode(VisionMode.OCR) }
                )
                ModeChip(
                    title = "Objects",
                    isSelected = currentMode == VisionMode.OBJECTS,
                    icon = Icons.Default.Widgets,
                    onClick = { viewModel.switchMode(VisionMode.OBJECTS) }
                )
                ModeChip(
                    title = "Spatial & Depth",
                    isSelected = currentMode == VisionMode.SPATIAL,
                    icon = Icons.Default.NearMe,
                    onClick = { viewModel.switchMode(VisionMode.SPATIAL) }
                )
                ModeChip(
                    title = "Ask AI",
                    isSelected = currentMode == VisionMode.ASK,
                    icon = Icons.Default.QuestionAnswer,
                    onClick = {
                        viewModel.switchMode(VisionMode.ASK)
                        viewModel.setAskDialogVisible(true)
                    }
                )
            }
        }

        // Result Card Drawer (if analysis is available)
        analysisResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .heightIn(max = 280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.title,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Drishti Description", result.detailedText))
                                    viewModel.ttsManager.speak("Description copied to clipboard")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy description to clipboard",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.stopSpeaking() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close result overlay",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Hazard Banner if detected
                    result.hazardWarning?.let { warning ->
                        Spacer(modifier = Modifier.height(8.dp))
                        HazardAlertBanner(warningMessage = warning)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.detailedText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    // Detected object badges
                    if (result.detectedObjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DetectedObjectsPills(items = result.detectedObjects)
                    }
                }
            }
        }

        // Bottom Controls: Audio Bar, Scan Button, Mic Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            AudioControlBar(
                isSpeaking = isSpeaking,
                onRepeat = { viewModel.repeatSpeech() },
                onStop = { viewModel.stopSpeaking() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Voice Command Button
                VoiceCommandButton(
                    isListening = isListening,
                    onClick = {
                        if (isListening) {
                            viewModel.stopListeningVoiceCommand()
                        } else {
                            viewModel.startListeningForVoiceCommand()
                        }
                    }
                )

                // Large Central Scan Button
                LargeScanButton(
                    isAnalyzing = isAnalyzing,
                    onClick = { viewModel.captureAndAnalyze() }
                )

                // Ask specific question shortcut
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.setAskDialogVisible(true) }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Ask question about surroundings"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Ask AI Dialog
        if (askDialogVisible) {
            var inputQuestion by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.setAskDialogVisible(false) },
                title = {
                    Text(
                        text = "Ask About Surroundings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Ask any specific visual question, like 'What color is the cup?' or 'Are there stairs?'",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputQuestion,
                            onValueChange = { inputQuestion = it },
                            placeholder = { Text("Type or speak question...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setAskDialogVisible(false)
                            viewModel.switchMode(VisionMode.ASK)
                            viewModel.setCustomQuestion(inputQuestion)
                            viewModel.captureAndAnalyze(inputQuestion)
                        },
                        enabled = inputQuestion.isNotBlank()
                    ) {
                        Text("Analyze & Answer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setAskDialogVisible(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
