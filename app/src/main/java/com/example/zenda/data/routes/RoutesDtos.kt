package com.example.zenda.data.routes

import com.google.gson.annotations.SerializedName

// --------- REQUEST (computeRoutes) ---------

data class ComputeRoutesRequestDto(
    @SerializedName("origin") val origin: WaypointDto,
    @SerializedName("destination") val destination: WaypointDto,
    @SerializedName("travelMode") val travelMode: String = "DRIVE",
    @SerializedName("routingPreference") val routingPreference: String = "TRAFFIC_AWARE",
    @SerializedName("computeAlternativeRoutes") val computeAlternativeRoutes: Boolean = false,
    @SerializedName("polylineEncoding") val polylineEncoding: String = "ENCODED_POLYLINE"
)

data class WaypointDto(
    @SerializedName("location") val location: LocationDto
)

data class LocationDto(
    @SerializedName("latLng") val latLng: LatLngDto
)

data class LatLngDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

// --------- RESPONSE (computeRoutes) ---------

data class ComputeRoutesResponseDto(
    @SerializedName("routes") val routes: List<RouteDto>?
)

data class RouteDto(
    @SerializedName("distanceMeters") val distanceMeters: Int?,
    @SerializedName("duration") val duration: String?, // ej: "345s"
    @SerializedName("polyline") val polyline: PolylineDto?,
    @SerializedName("legs") val legs: List<RouteLegDto>?
)

data class PolylineDto(
    @SerializedName("encodedPolyline") val encodedPolyline: String?
)

data class RouteLegDto(
    @SerializedName("distanceMeters") val distanceMeters: Int?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("steps") val steps: List<RouteStepDto>?
)

data class RouteStepDto(
    @SerializedName("navigationInstruction") val navigationInstruction: NavigationInstructionDto?
)

data class NavigationInstructionDto(
    @SerializedName("maneuver") val maneuver: String?,
    @SerializedName("instructions") val instructions: String?
)

