package com.example.zenda.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {

    // 📍 Ubicación base (Arequipa)
    val arequipa = LatLng(-16.4090, -71.5375)

    // 🎥 Estado de cámara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(arequipa, 14f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {

        // 🟢 RUTA SEGURA
        Polyline(
            points = viewModel.getSafeRoute(),
            color = Color.Green,
            width = 8f
        )

        // 🔴 ZONAS PELIGROSAS
        viewModel.getDangerZones().forEach { zone ->
            Circle(
                center = zone,
                radius = 200.0,
                fillColor = Color.Red.copy(alpha = 0.3f),
                strokeColor = Color.Red
            )
        }
    }
}