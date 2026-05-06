package com.example.zenda.ui.map

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapDestinationMarker(
    val id: String,
    val title: String,
    val position: LatLng
)

class MapViewModel : ViewModel() {

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _userBearingDegrees = MutableStateFlow<Float?>(null)
    val userBearingDegrees: StateFlow<Float?> = _userBearingDegrees.asStateFlow()

    private val _safeRoutes = MutableStateFlow(
        listOf(
            LatLng(-16.4090, -71.5375),
            LatLng(-16.4100, -71.5385),
            LatLng(-16.4110, -71.5395),
            LatLng(-16.4122, -71.5410)
        )
    )
    val safeRoutes: StateFlow<List<LatLng>> = _safeRoutes.asStateFlow()

    private val _dangerZones = MutableStateFlow(
        listOf(
            LatLng(-16.4120, -71.5400),
            LatLng(-16.4130, -71.5410)
        )
    )
    val dangerZones: StateFlow<List<LatLng>> = _dangerZones.asStateFlow()

    /**
     * Puntos de interés seleccionables como destino de navegación (reportes / lugares).
     */
    private val _mapDestinations = MutableStateFlow(
        listOf(
            MapDestinationMarker("1", "Reporte: zona céntrica", LatLng(-16.4105, -71.5390)),
            MapDestinationMarker("2", "Punto de encuentro", LatLng(-16.4080, -71.5360))
        )
    )
    val mapDestinations: StateFlow<List<MapDestinationMarker>> = _mapDestinations.asStateFlow()

    fun updateUserLocation(location: LatLng, bearingDegrees: Float? = null) {
        _userLocation.value = location
        if (bearingDegrees != null) {
            _userBearingDegrees.value = bearingDegrees
        }
    }
}
