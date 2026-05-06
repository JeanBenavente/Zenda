package com.example.zenda.data.routes

import com.google.android.gms.maps.model.LatLng

/**
 * Contexto futuro: rutas seguras (evitar zonas peligrosas / reportes).
 * Routes API no soporta evitar polígonos arbitrarios directamente; esto queda listo
 * para integrar backend/IA, waypoints o un motor propio más adelante.
 */
data class RoutingContext(
    val preferSafeRoute: Boolean = true,
    val dangerZoneCenters: List<LatLng> = emptyList(),
    val recentReportPositions: List<LatLng> = emptyList()
)

