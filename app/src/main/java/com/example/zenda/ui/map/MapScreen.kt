package com.example.zenda.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onStartNavigation: (LatLng, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsState()
    val safeRoutes by viewModel.safeRoutes.collectAsState()
    val dangerZones by viewModel.dangerZones.collectAsState()
    val mapDestinations by viewModel.mapDestinations.collectAsState()
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    var pendingMapTap by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        kotlin.runCatching { fusedLocationClient.lastLocation.await() }
            .getOrNull()
            ?.let {
                viewModel.updateUserLocation(
                    LatLng(it.latitude, it.longitude),
                    it.bearing.takeIf { b -> it.hasBearing() }
                )
            }
    }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            onDispose { }
        } else {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3_000L
            ).setMinUpdateIntervalMillis(1_500L).build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    viewModel.updateUserLocation(
                        LatLng(location.latitude, location.longitude),
                        location.bearing.takeIf { location.hasBearing() }
                    )
                }
            }

            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                context.mainLooper
            )

            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    pendingMapTap?.let { tap ->
        AlertDialog(
            onDismissRequest = { pendingMapTap = null },
            title = { Text("Navegar aquí") },
            text = { Text("¿Usar este punto como destino en Zenda?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartNavigation(tap, "Ubicación en mapa")
                        pendingMapTap = null
                    }
                ) {
                    Text("Navegar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMapTap = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            !hasLocationPermission -> {
                MapPermissionPlaceholder(
                    onAllowLocation = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
            }

            userLocation == null -> {
                MapLocationLoadingPlaceholder()
            }

            else -> {
                val location = userLocation!!
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(location, 16f)
                }

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false
                        ),
                        onMapClick = { latLng ->
                            pendingMapTap = latLng
                        }
                    ) {
                        mapDestinations.forEach { poi ->
                            Marker(
                                state = MarkerState(position = poi.position),
                                title = poi.title,
                                snippet = "Toca el globo para navegar",
                                onInfoWindowClick = {
                                    onStartNavigation(poi.position, poi.title)
                                },
                                onClick = {
                                    onStartNavigation(poi.position, poi.title)
                                    true
                                }
                            )
                        }

                        Polyline(
                            points = safeRoutes,
                            color = Color(0xFF2E7D32),
                            width = 10f
                        )

                        dangerZones.forEach { zone ->
                            Circle(
                                center = zone,
                                radius = 220.0,
                                fillColor = Color(0x66FF5252),
                                strokeColor = Color(0xFFFF5252),
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    RowMapHint()
                }

                FloatingActionButton(
                    onClick = {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(location, 16f)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 200.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Centrar en mi ubicación"
                    )
                }
            }
        }
    }
}

@Composable
private fun RowMapHint() {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Navigation,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "Toca el mapa o un marcador para iniciar navegación dentro de Zenda.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MapPermissionPlaceholder(
    onAllowLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
        )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.padding(horizontal = 28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tu mapa en Zenda",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Activa la ubicación para abrir el mapa centrado en donde estás, con rutas y zonas de alerta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onAllowLocation,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Permitir ubicación")
                }
            }
        }
    }
}

@Composable
private fun MapLocationLoadingPlaceholder(modifier: Modifier = Modifier) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
        )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.padding(horizontal = 28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Obteniendo tu ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Un momento: el mapa se abrirá directamente en tu posición.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
