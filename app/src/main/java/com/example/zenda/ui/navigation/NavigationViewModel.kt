package com.example.zenda.ui.navigation

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zenda.data.routes.NavigationRoute
import com.example.zenda.data.routes.NavigationStep
import com.example.zenda.data.routes.RoutesRepository
import com.example.zenda.data.routes.RoutesRepositoryImpl
import com.example.zenda.data.routes.RoutingContext
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de navegación turn-by-turn: ruta, tiempos, instrucción y recálculo optimizado.
 */
data class NavigationUiState(
    val destination: LatLng? = null,
    val destinationTitle: String = "",
    val routePoints: List<LatLng> = emptyList(),
    val steps: List<NavigationStep> = emptyList(),
    val durationSeconds: Int? = null,
    val distanceMeters: Int? = null,
    val currentInstruction: String = "",
    val isLoadingRoute: Boolean = false,
    val error: String? = null,
    val followCamera: Boolean = true,
    val userBearing: Float = 0f
)

class NavigationViewModel(
    private val repository: RoutesRepository = RoutesRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var lastRecalcPosition: LatLng? = null
    private var lastRecalcTimeMs: Long = 0L
    private var dangerCenters: List<LatLng> = emptyList()
    private var reportPositions: List<LatLng> = emptyList()

    fun startSession(
        destination: LatLng,
        title: String,
        dangerZones: List<LatLng> = emptyList(),
        reports: List<LatLng> = emptyList()
    ) {
        dangerCenters = dangerZones
        reportPositions = reports
        lastRecalcPosition = null
        lastRecalcTimeMs = 0L
        _uiState.value = NavigationUiState(
            destination = destination,
            destinationTitle = title,
            currentInstruction = "Calculando ruta…"
        )
    }

    fun cancelNavigation() {
        lastRecalcPosition = null
        lastRecalcTimeMs = 0L
        dangerCenters = emptyList()
        reportPositions = emptyList()
        _uiState.value = NavigationUiState()
    }

    fun setFollowCamera(follow: Boolean) {
        _uiState.update { it.copy(followCamera = follow) }
    }

    fun onUserLocationUpdated(location: LatLng, bearingFromGps: Float?) {
        val dest = _uiState.value.destination ?: return
        bearingFromGps?.let { b -> _uiState.update { s -> s.copy(userBearing = b) } }

        if (_uiState.value.routePoints.isEmpty()) {
            if (!_uiState.value.isLoadingRoute && _uiState.value.error == null) {
                viewModelScope.launch { requestRoute(location, dest, force = true) }
            }
        } else {
            maybeRecalculateRoute(location)
            refreshInstruction(location)
        }
    }

    fun retryRoute(origin: LatLng) {
        val dest = _uiState.value.destination ?: return
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch { requestRoute(origin, dest, force = true) }
    }

    private fun refreshInstruction(user: LatLng) {
        val steps = _uiState.value.steps
        if (steps.isEmpty()) return
        val text = pickInstruction(user, steps) ?: return
        if (text != _uiState.value.currentInstruction) {
            _uiState.update { it.copy(currentInstruction = text) }
        }
    }

    private fun pickInstruction(user: LatLng, steps: List<NavigationStep>): String? {
        // Routes API entrega instrucciones por step, pero este MVP no calcula “step actual”
        // por geometría. Usamos la primera instrucción disponible como guía básica.
        return steps.firstOrNull()?.instruction?.takeIf { it.isNotBlank() }
            ?: "Continúa hacia el destino"
    }

    private fun maybeRecalculateRoute(current: LatLng) {
        if (_uiState.value.routePoints.isEmpty()) return
        val dest = _uiState.value.destination ?: return
        val now = System.currentTimeMillis()
        val last = lastRecalcPosition
        if (last == null) {
            lastRecalcPosition = current
            lastRecalcTimeMs = now
            return
        }
        val moved = FloatArray(1)
        Location.distanceBetween(
            last.latitude, last.longitude,
            current.latitude, current.longitude,
            moved
        )
        if (moved[0] >= MIN_RECURSE_METERS && now - lastRecalcTimeMs >= MIN_RECURSE_INTERVAL_MS) {
            lastRecalcPosition = current
            lastRecalcTimeMs = now
            viewModelScope.launch { requestRoute(current, dest, force = false) }
        }
    }

    private suspend fun requestRoute(origin: LatLng, destination: LatLng, force: Boolean) {
        if (!force && _uiState.value.isLoadingRoute) return
        _uiState.update { it.copy(isLoadingRoute = true, error = null) }
        val ctx = RoutingContext(
            preferSafeRoute = true,
            dangerZoneCenters = dangerCenters,
            recentReportPositions = reportPositions
        )
        val result = repository.getDrivingRoute(origin, destination, ctx)
        result.fold(
            onSuccess = { route -> applyRoute(route) },
            onFailure = { e ->
                _uiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        error = e.message ?: "Error al obtener la ruta"
                    )
                }
            }
        )
    }

    private fun applyRoute(route: NavigationRoute) {
        val title = _uiState.value.destinationTitle
        val dest = _uiState.value.destination
        _uiState.update { prev ->
            prev.copy(
                routePoints = route.points,
                steps = route.steps,
                durationSeconds = route.durationSeconds,
                distanceMeters = route.distanceMeters,
                currentInstruction = route.steps.firstOrNull()?.instruction
                    ?: "Continúa hacia el destino",
                isLoadingRoute = false,
                error = null,
                destination = dest,
                destinationTitle = title
            )
        }
        lastRecalcPosition = null
        lastRecalcTimeMs = System.currentTimeMillis()
    }

    companion object {
        private const val MIN_RECURSE_METERS = 85f
        private const val MIN_RECURSE_INTERVAL_MS = 28_000L
    }
}
