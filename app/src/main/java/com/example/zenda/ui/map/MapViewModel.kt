package com.example.zenda.ui.map

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class MapViewModel : ViewModel() {

    // 🟢 RUTA SEGURA
    fun getSafeRoute(): List<LatLng> {
        return listOf(
            LatLng(-16.4090, -71.5375),
            LatLng(-16.4100, -71.5385),
            LatLng(-16.4110, -71.5395)
        )
    }

    // 🔴 ZONAS PELIGROSAS
    fun getDangerZones(): List<LatLng> {
        return listOf(
            LatLng(-16.4120, -71.5400),
            LatLng(-16.4130, -71.5410)
        )
    }
}