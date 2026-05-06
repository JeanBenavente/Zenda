package com.example.zenda.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.zenda.ui.map.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.awaitCancellation

/**
 * Navegación turn-by-turn in-app: mapa a pantalla completa, panel inferior y seguimiento de cámara.
 */
@Composable
fun NavigationScreen(
    mapViewModel: MapViewModel,
    navigationViewModel: NavigationViewModel,
    destination: LatLng,
    destinationTitle: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userLocation by mapViewModel.userLocation.collectAsState()
    val userBearing by mapViewModel.userBearingDegrees.collectAsState()
    val dangerZones by mapViewModel.dangerZones.collectAsState()
    val mapDestinations by mapViewModel.mapDestinations.collectAsState()
    val uiState by navigationViewModel.uiState.collectAsState()

    LaunchedEffect(destination.latitude, destination.longitude, destinationTitle) {
        navigationViewModel.startSession(
            destination = destination,
            title = destinationTitle,
            dangerZones = dangerZones,
            reports = mapDestinations.map { it.position }
        )
    }

    LaunchedEffect(userLocation, userBearing) {
        val u = userLocation ?: return@LaunchedEffect
        navigationViewModel.onUserLocationUpdated(u, userBearing)
    }

    val cameraPositionState = rememberCameraPositionState {
        val start = userLocation ?: destination
        position = CameraPosition.fromLatLngZoom(start, 17f)
    }

    LaunchedEffect(userLocation, uiState.followCamera, uiState.userBearing) {
        val u = userLocation ?: return@LaunchedEffect
        if (!uiState.followCamera) return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(u)
                    .zoom(17f)
                    .tilt(50f)
                    .bearing(uiState.userBearing)
                    .build()
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            MapEffect(Unit) { map ->
                val listener = com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener { reason ->
                    if (reason == com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        navigationViewModel.setFollowCamera(false)
                    }
                }
                map.setOnCameraMoveStartedListener(listener)
                try {
                    awaitCancellation()
                } finally {
                    map.setOnCameraMoveStartedListener(null)
                }
            }
            if (uiState.routePoints.isNotEmpty()) {
                Polyline(
                    points = uiState.routePoints,
                    color = Color(0xFF1565C0),
                    width = 12f
                )
            }

            userLocation?.let { u ->
                Marker(
                    state = MarkerState(position = u),
                    title = "Tu posición",
                    rotation = uiState.userBearing,
                    flat = true
                )
            }

            Marker(
                state = MarkerState(position = destination),
                title = uiState.destinationTitle.ifBlank { "Destino" }
            )
        }

        NavigationBottomPanel(
            uiState = uiState,
            onCancel = onCancel,
            onRecenter = {
                navigationViewModel.setFollowCamera(true)
                userLocation?.let { u ->
                    cameraPositionState.move(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(u)
                                .zoom(17f)
                                .tilt(50f)
                                .bearing(uiState.userBearing)
                                .build()
                        )
                    )
                }
            },
            onRetry = {
                userLocation?.let { navigationViewModel.retryRoute(it) }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        FloatingActionButton(
            onClick = {
                navigationViewModel.setFollowCamera(true)
                userLocation?.let { u ->
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(u, 17f)
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 200.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            )
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Seguir mi ubicación")
        }
    }
}

@Composable
private fun NavigationBottomPanel(
    uiState: NavigationUiState,
    onCancel: () -> Unit,
    onRecenter: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.destinationTitle.ifBlank { "Destino" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Navegación en Zenda",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar navegación")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text(
                        "Tiempo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDuration(uiState.durationSeconds),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        "Distancia",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDistance(uiState.distanceMeters),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.isLoadingRoute) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = uiState.currentInstruction.ifBlank { "Preparando indicaciones…" },
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("Reintentar ruta")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRecenter,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Seguir posición")
                }
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Cancelar")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "—"
    val m = seconds / 60
    val s = seconds % 60
    return if (m >= 60) {
        val h = m / 60
        val rm = m % 60
        "${h}h ${rm}m"
    } else {
        "${m} min"
    }
}

private fun formatDistance(meters: Int?): String {
    if (meters == null || meters <= 0) return "—"
    return if (meters >= 1000) {
        "%.1f km".format(meters / 1000f)
    } else {
        "$meters m"
    }
}
