package com.kai.colorlens.ui

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kai.colorlens.ColorViewModel
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorLensScreen(viewModel: ColorViewModel) {
    val state by viewModel.state.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ColorLens") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Kamera-Vorschau mit Farbanalyse
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CameraPreview(
                    onColorDetected = { r, g, b -> viewModel.updateColor(r, g, b) }
                )

                // Fadenkreuz in der Mitte
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .border(2.dp, Color.White, CircleShape)
                )

                // Farbwert-Overlay unten
                ColorInfoOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    hex = state.currentHex,
                    rgb = state.currentRgb,
                    hsl = state.currentHsl,
                    onSave = { viewModel.saveCurrentColor() }
                )
            }

            // Gespeicherte Farben
            if (state.savedColors.isNotEmpty()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Palette (${state.savedColors.size})", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showExportDialog = true }) {
                            Text("Exportieren")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(state.savedColors) { index, saved ->
                            val color = parseHexColor(saved.hex)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { viewModel.removeColor(index) }
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onJson = { viewModel.exportAsJson() },
            onCss = { viewModel.exportAsCss() },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun ColorInfoOverlay(
    modifier: Modifier,
    hex: String,
    rgb: Triple<Int, Int, Int>,
    hsl: Triple<Float, Float, Float>,
    onSave: () -> Unit
) {
    val color = parseHexColor(hex)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Column {
                Text(hex, style = MaterialTheme.typography.titleMedium)
                Text(
                    "RGB: ${rgb.first}, ${rgb.second}, ${rgb.third}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "HSL: ${"%.0f".format(hsl.first)}°, ${"%.0f".format(hsl.second)}%, ${"%.0f".format(hsl.third)}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Add, contentDescription = "Farbe speichern")
            }
        }
    }
}

@Composable
private fun CameraPreview(onColorDetected: (Int, Int, Int) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    extractCenterPixel(imageProxy, onColorDetected)
                    imageProxy.close()
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun extractCenterPixel(imageProxy: ImageProxy, onColorDetected: (Int, Int, Int) -> Unit) {
    // YUV_420_888 -> RGB Konvertierung fuer den Mittelpixel
    val buffer: ByteBuffer = imageProxy.planes[0].buffer
    val width = imageProxy.width
    val height = imageProxy.height
    val cx = width / 2
    val cy = height / 2
    val rowStride = imageProxy.planes[0].rowStride
    val pixelStride = imageProxy.planes[0].pixelStride

    val yIndex = cy * rowStride + cx * pixelStride
    buffer.position(yIndex)
    val y = buffer.get().toInt() and 0xFF

    val uvBuffer = imageProxy.planes[1].buffer
    val uvRowStride = imageProxy.planes[1].rowStride
    val uvPixelStride = imageProxy.planes[1].pixelStride
    val uvIndex = (cy / 2) * uvRowStride + (cx / 2) * uvPixelStride
    uvBuffer.position(uvIndex)
    val u = (uvBuffer.get().toInt() and 0xFF) - 128
    val v: Int
    val vBuffer = imageProxy.planes[2].buffer
    vBuffer.position(uvIndex)
    v = (vBuffer.get().toInt() and 0xFF) - 128

    val r = (y + 1.402 * v).toInt().coerceIn(0, 255)
    val g = (y - 0.344136 * u - 0.714136 * v).toInt().coerceIn(0, 255)
    val b = (y + 1.772 * u).toInt().coerceIn(0, 255)

    onColorDetected(r, g, b)
}

@Composable
private fun ExportDialog(
    onJson: () -> String,
    onCss: () -> String,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { content = onJson(); format = "JSON" }) { Text("JSON") }
                    Button(onClick = { content = onCss(); format = "CSS" }) { Text("CSS") }
                }
                if (content.isNotEmpty()) {
                    Text(format, style = MaterialTheme.typography.labelMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            content,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schliessen") } }
    )
}

private fun parseHexColor(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color.Gray
    }
}
