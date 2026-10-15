package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScanHistoryScreen
import com.example.ui.screens.UserSettingsScreen
import com.example.ui.screens.VisionHomeScreen
import com.example.ui.theme.VisionVoiceTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.VisionViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VisionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()

            var hasPermissions by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                hasPermissions = (permissions[Manifest.permission.CAMERA] == true)
            }

            LaunchedEffect(Unit) {
                if (!hasPermissions) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            }

            VisionVoiceTheme(contrastTheme = settings.contrastTheme) {
                // Back button returns to Vision home screen if on secondary tab
                BackHandler(enabled = currentTab != AppTab.VISION) {
                    viewModel.selectTab(AppTab.VISION)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .height(72.dp)
                                .testTag("main_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.VISION,
                                onClick = { viewModel.selectTab(AppTab.VISION) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Vision",
                                        fontWeight = if (currentTab == AppTab.VISION) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "Vision assistant screen tab"
                                }
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.HISTORY,
                                onClick = { viewModel.selectTab(AppTab.HISTORY) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "History",
                                        fontWeight = if (currentTab == AppTab.HISTORY) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "Scan history tab"
                                }
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.DASHBOARD,
                                onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Guide",
                                        fontWeight = if (currentTab == AppTab.DASHBOARD) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "Dashboard and guide tab"
                                }
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SETTINGS,
                                onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Settings",
                                        fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "Accessibility settings tab"
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            AppTab.VISION -> VisionHomeScreen(
                                viewModel = viewModel,
                                hasCameraPermission = hasPermissions,
                                onRequestPermission = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                }
                            )
                            AppTab.HISTORY -> ScanHistoryScreen(viewModel = viewModel)
                            AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                            AppTab.SETTINGS -> UserSettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
