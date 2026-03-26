package com.kai.colorlens

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.kai.colorlens.ui.ColorLensScreen
import com.kai.colorlens.ui.theme.ColorLensTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ColorViewModel by viewModels()
    private var cameraGranted by mutableStateOf(false)

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermission.launch(Manifest.permission.CAMERA)

        setContent {
            ColorLensTheme {
                if (cameraGranted) {
                    ColorLensScreen(viewModel = viewModel)
                } else {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Kamera-Berechtigung") },
                        text = { Text("ColorLens benoetigt Kamera-Zugriff.") },
                        confirmButton = {
                            TextButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) {
                                Text("Berechtigung anfordern")
                            }
                        }
                    )
                }
            }
        }
    }
}
