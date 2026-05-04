package com.example.zenda

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.zenda.ui.map.MapScreen
import com.example.zenda.ui.map.MapViewModel
import com.example.zenda.ui.panic.PanicButton

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZendaTheme {

                Scaffold(
                    containerColor = Color.White,
                    floatingActionButton = {
                        PanicButton(
                            onClick = {
                                Log.d("Zenda", "🚨 ALERTA ACTIVADA")
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        MapScreen(
                            viewModel = mapViewModel,
                            modifier = Modifier.fillMaxSize()
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun ZendaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            surface = Color.White,
            background = Color(0xFFF5FAFF)
        ),
        content = content
    )
}