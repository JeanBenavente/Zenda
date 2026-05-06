package com.example.zenda.data.routes

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

interface RoutesRepository {
    suspend fun getDrivingRoute(
        origin: LatLng,
        destination: LatLng,
        context: RoutingContext = RoutingContext()
    ): Result<NavigationRoute>
}

class RoutesRepositoryImpl(
    private val api: RoutesApiService = RoutesRetrofitModule.api
) : RoutesRepository {

    override suspend fun getDrivingRoute(
        origin: LatLng,
        destination: LatLng,
        context: RoutingContext
    ): Result<NavigationRoute> = withContext(Dispatchers.IO) {
        val apiKey = com.example.zenda.BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("MAPS_API_KEY vacía. Configura Routes API en Google Cloud y local.properties.")
            )
        }

        // Hook futuro: usar context.preferSafeRoute + zonas/reportes para decidir rutas/waypoints.
        @Suppress("UNUSED_VARIABLE")
        val _ctx = context

        try {
            val body = ComputeRoutesRequestDto(
                origin = WaypointDto(
                    location = LocationDto(
                        latLng = LatLngDto(origin.latitude, origin.longitude)
                    )
                ),
                destination = WaypointDto(
                    location = LocationDto(
                        latLng = LatLngDto(destination.latitude, destination.longitude)
                    )
                )
            )

            val response = api.computeRoutes(body)
            val route = response.routes?.firstOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Sin rutas"))

            val encoded = route.polyline?.encodedPolyline
                ?: return@withContext Result.failure(IllegalStateException("Sin polyline"))

            val points = PolylineDecoder.decode(encoded)
            val leg = route.legs?.firstOrNull()

            val durationSeconds = parseDurationSeconds(route.duration ?: leg?.duration) ?: 0
            val distanceMeters = route.distanceMeters ?: leg?.distanceMeters ?: 0

            val steps = leg?.steps.orEmpty().mapNotNull { step ->
                val nav = step.navigationInstruction ?: return@mapNotNull null
                val instruction = nav.instructions?.trim().orEmpty()
                NavigationStep(
                    instruction = instruction.ifBlank { maneuverToText(nav.maneuver) },
                    maneuver = nav.maneuver
                )
            }

            Result.success(
                NavigationRoute(
                    points = points,
                    durationSeconds = durationSeconds,
                    distanceMeters = distanceMeters,
                    steps = steps
                )
            )
        } catch (e: HttpException) {
            Result.failure(IOException("Error HTTP ${e.code()}: ${e.message()}", e))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDurationSeconds(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        // Formato típico en Routes: "123s"
        return raw.removeSuffix("s").toIntOrNull()
    }

    private fun maneuverToText(maneuver: String?): String {
        if (maneuver.isNullOrBlank()) return "Continúa recto"
        return when {
            maneuver.contains("TURN_RIGHT", ignoreCase = true) -> "Gira a la derecha"
            maneuver.contains("TURN_LEFT", ignoreCase = true) -> "Gira a la izquierda"
            maneuver.contains("UTURN", ignoreCase = true) -> "Cambio de sentido"
            maneuver.contains("MERGE", ignoreCase = true) -> "Incorpórate"
            maneuver.contains("ROUNDABOUT", ignoreCase = true) -> "Rotonda"
            maneuver.contains("STRAIGHT", ignoreCase = true) -> "Continúa recto"
            else -> "Continúa"
        }
    }
}

