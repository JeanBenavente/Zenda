package com.example.zenda.data.routes

import com.google.android.gms.maps.model.LatLng

/**
 * Modelo de dominio para la UI: ruta (polyline) + métricas + instrucciones básicas.
 */
data class NavigationRoute(
    val points: List<LatLng>,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val steps: List<NavigationStep>
)

data class NavigationStep(
    val instruction: String,
    val maneuver: String?
)

