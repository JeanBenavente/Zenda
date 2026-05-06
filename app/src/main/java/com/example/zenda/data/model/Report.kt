package com.example.zenda.data.model

import android.net.Uri

enum class ReportImageSource {
    NONE,
    GALLERY,
    CAMERA
}

enum class ReportCategory(val label: String) {
    ROBO("Robo"),
    ACCIDENTE("Accidente"),
    PELEA("Pelea"),
    INCENDIO("Incendio"),
    PERSONA_SOSPECHOSA("Persona sospechosa"),
    OTRO("Otro")
}

enum class DangerLevel(val label: String) {
    BAJO("Bajo"),
    MEDIO("Medio"),
    ALTO("Alto")
}

data class Report(
    val id: String,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val dangerLevel: DangerLevel,
    val latitude: Double,
    val longitude: Double,
    val imageUri: Uri?,
    val imageSource: ReportImageSource,
    val createdAtEpochMs: Long
)
