package com.kai.colorlens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavedColor(val hex: String, val label: String = "")

data class ColorLensState(
    val currentHex: String = "#000000",
    val currentRgb: Triple<Int, Int, Int> = Triple(0, 0, 0),
    val currentHsl: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val savedColors: List<SavedColor> = emptyList()
)

class ColorViewModel : ViewModel() {

    private val _state = MutableStateFlow(ColorLensState())
    val state: StateFlow<ColorLensState> = _state.asStateFlow()

    fun updateColor(r: Int, g: Int, b: Int) {
        val hex = "#%02X%02X%02X".format(r, g, b)
        val hsl = rgbToHsl(r, g, b)
        _state.value = _state.value.copy(
            currentHex = hex,
            currentRgb = Triple(r, g, b),
            currentHsl = hsl
        )
    }

    fun saveCurrentColor() {
        val current = SavedColor(hex = _state.value.currentHex)
        val updated = _state.value.savedColors + current
        _state.value = _state.value.copy(savedColors = updated)
    }

    fun removeColor(index: Int) {
        val updated = _state.value.savedColors.toMutableList().also { it.removeAt(index) }
        _state.value = _state.value.copy(savedColors = updated)
    }

    fun exportAsJson(): String {
        val array = JSONArray()
        _state.value.savedColors.forEach { color ->
            array.put(JSONObject().apply {
                put("hex", color.hex)
                put("label", color.label)
            })
        }
        return array.toString(2)
    }

    fun exportAsCss(): String {
        return buildString {
            appendLine(":root {")
            _state.value.savedColors.forEachIndexed { i, color ->
                appendLine("  --color-${i + 1}: ${color.hex};")
            }
            appendLine("}")
        }
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val l = (max + min) / 2f
        if (max == min) return Triple(0f, 0f, l)
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            rf -> ((gf - bf) / d + if (gf < bf) 6f else 0f) / 6f
            gf -> ((bf - rf) / d + 2f) / 6f
            else -> ((rf - gf) / d + 4f) / 6f
        }
        return Triple(h * 360f, s * 100f, l * 100f)
    }
}
