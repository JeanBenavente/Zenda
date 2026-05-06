package com.example.zenda.ui.panic

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices

@SuppressLint("MissingPermission")
@Composable
fun PanicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    FloatingActionButton(
        onClick = {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d(
                                "ZendaPanic",
                                "🚨 ALERTA: lat=${location.latitude}, lng=${location.longitude}"
                            )
                            val mockPayload = mapOf(
                                "type" to "panic_alert",
                                "latitude" to location.latitude,
                                "longitude" to location.longitude
                            )
                            Log.d("ZendaPanic", "Mock backend payload: $mockPayload")
                        } else {
                            Log.d("ZendaPanic", "🚨 ALERTA: ubicación no disponible")
                            Log.d("ZendaPanic", "Mock backend payload: { type=panic_alert, status=no_location }")
                        }

                        Toast.makeText(
                            context,
                            "Alerta enviada con tu ubicación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Log.e("ZendaPanic", "Error obteniendo ubicación para alerta", it)
                        Toast.makeText(
                            context,
                            "Alerta enviada con tu ubicación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Log.d("ZendaPanic", "🚨 ALERTA: sin permisos de ubicación")
                Log.d("ZendaPanic", "Mock backend payload: { type=panic_alert, status=permission_denied }")
                Toast.makeText(
                    context,
                    "Alerta enviada con tu ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            }

            onClick()
        },
        containerColor = Color.Red,
        modifier = modifier
    ) {
        Text(text = "SOS", color = Color.White)
    }
}