package com.maxs2026.liquidglasscc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiquidGlassApp() }
    }
}

@Composable
private fun LiquidGlassApp() {
    var darkMode by remember { mutableStateOf(true) }
    var glass by remember { mutableFloatStateOf(0.72f) }
    var accent by remember { mutableStateOf(Color(0xFF9BCBFF)) }
    val background = if (darkMode) Color(0xFF101522) else Color(0xFFE9F0FA)
    val foreground = if (darkMode) Color.White else Color(0xFF172033)

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(background, Color(0xFF273A59), background))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Control Center", color = foreground, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Liquid Glass • Customize your space", color = foreground.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                    IconButton(onClick = { darkMode = !darkMode }) {
                        Icon(Icons.Default.Settings, contentDescription = "Toggle appearance", tint = foreground)
                    }
                }
                GlassPanel(glass) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(190.dp)) {
                        items(listOf("Wi-Fi", "Bluetooth", "Airplane", "Flashlight")) { label ->
                            val icon = when (label) {
                                "Wi-Fi" -> Icons.Default.Wifi
                                "Bluetooth" -> Icons.Default.Bluetooth
                                "Airplane" -> Icons.Default.Flight
                                else -> Icons.Default.FlashlightOn
                            }
                            var enabled by remember { mutableStateOf(label == "Wi-Fi") }
                            Surface(onClick = { enabled = !enabled }, shape = RoundedCornerShape(18.dp), color = if (enabled) accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.10f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = foreground)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, color = foreground, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                GlassPanel(glass) {
                    Text("Display & sound", color = foreground, fontWeight = FontWeight.SemiBold)
                    var brightness by remember { mutableFloatStateOf(0.75f) }
                    Text("Brightness", color = foreground.copy(alpha = 0.8f), fontSize = 13.sp)
                    Slider(value = brightness, onValueChange = { brightness = it }, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    var volume by remember { mutableFloatStateOf(0.55f) }
                    Text("Volume", color = foreground.copy(alpha = 0.8f), fontSize = 13.sp)
                    Slider(value = volume, onValueChange = { volume = it }, colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                }
                GlassPanel(glass) {
                    Text("Personalize", color = foreground, fontWeight = FontWeight.SemiBold)
                    Text("Glass transparency", color = foreground.copy(alpha = 0.8f), fontSize = 13.sp)
                    Slider(value = glass, onValueChange = { glass = it }, valueRange = 0.2f..1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark appearance", color = foreground, modifier = Modifier.weight(1f))
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(Color(0xFF9BCBFF), Color(0xFFC7A5FF), Color(0xFF8DE0C0), Color(0xFFFFC58A)).forEach { color ->
                            Surface(onClick = { accent = color }, modifier = Modifier.size(34.dp), shape = RoundedCornerShape(50), color = color, border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f))) {}
                        }
                    }
                }
                Text("Prototype controls • Some Android system settings need special permissions", color = foreground.copy(alpha = 0.65f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GlassPanel(alpha: Float, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.10f + alpha * 0.10f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shadowElevation = 12.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
