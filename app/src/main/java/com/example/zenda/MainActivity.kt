package com.example.zenda

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.example.zenda.ui.login.LoginScreen
import com.example.zenda.ui.map.MapScreen
import com.example.zenda.ui.map.MapViewModel
import com.example.zenda.ui.navigation.NavigationScreen
import com.example.zenda.ui.navigation.NavigationViewModel
import com.example.zenda.ui.panic.PanicButton
import com.example.zenda.ui.report.CreateReportFab
import com.example.zenda.ui.report.CreateReportScreen
import com.example.zenda.ui.report.ReportViewModel

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModels()
    private val reportViewModel: ReportViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZendaTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                var showCreateReport by remember { mutableStateOf(false) }

                AnimatedContent(
                    targetState = isLoggedIn,
                    transitionSpec = {
                        if (targetState) {
                            (fadeIn() + slideInHorizontally { it / 6 }) togetherWith
                                (fadeOut() + slideOutHorizontally { -it / 10 })
                        } else {
                            (fadeIn() + slideInHorizontally { -it / 6 }) togetherWith
                                (fadeOut() + slideOutHorizontally { it / 10 })
                        }
                    },
                    label = "auth_nav",
                    modifier = Modifier.fillMaxSize()
                ) { loggedIn ->
                    if (!loggedIn) {
                        LoginScreen(
                            onLoginSuccess = { isLoggedIn = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        var navSession by remember {
                            mutableStateOf<Pair<LatLng, String>?>(null)
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (navSession != null) {
                                val (dest, title) = navSession!!
                                NavigationScreen(
                                    mapViewModel = mapViewModel,
                                    navigationViewModel = navigationViewModel,
                                    destination = dest,
                                    destinationTitle = title,
                                    onCancel = {
                                        navSession = null
                                        navigationViewModel.cancelNavigation()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Scaffold(
                                    containerColor = Color.White,
                                    floatingActionButton = {
                                        if (!showCreateReport) {
                                            Column(
                                                modifier = Modifier
                                                    .navigationBarsPadding()
                                                    .padding(end = 4.dp, bottom = 4.dp),
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                CreateReportFab(
                                                    onClick = { showCreateReport = true }
                                                )
                                                PanicButton(
                                                    onClick = {
                                                        Log.d("Zenda", "🚨 ALERTA ACTIVADA")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        AnimatedContent(
                                            targetState = showCreateReport,
                                            transitionSpec = {
                                                if (targetState) {
                                                    (fadeIn() + slideInHorizontally { it / 8 }) togetherWith
                                                        (fadeOut() + slideOutHorizontally { -it / 12 })
                                                } else {
                                                    (fadeIn() + slideInHorizontally { -it / 12 }) togetherWith
                                                        (fadeOut() + slideOutHorizontally { it / 8 })
                                                }
                                            },
                                            label = "main_nav"
                                        ) { createReport ->
                                            if (createReport) {
                                                CreateReportScreen(
                                                    viewModel = reportViewModel,
                                                    onBack = { showCreateReport = false },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                MapScreen(
                                                    viewModel = mapViewModel,
                                                    onStartNavigation = { lat, title ->
                                                        navSession = lat to title
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZendaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE3F2FD),
            onPrimaryContainer = Color(0xFF0D47A1),
            secondary = Color(0xFF00838F),
            onSecondary = Color.White,
            tertiary = Color(0xFF5C6BC0),
            surface = Color.White,
            onSurfaceVariant = Color(0xFF546E7A),
            background = Color(0xFFF5FAFF)
        ),
        content = content
    )
}
