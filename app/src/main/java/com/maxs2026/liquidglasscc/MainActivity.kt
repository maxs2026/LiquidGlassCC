package com.maxs2026.liquidglasscc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var glass by remember { mutableFloatStateOf(0.58f) }
    var accent by remember { mutableStateOf(Color(0xFF9CCBFF)) }
    val ink = if (darkMode) Color(0xFFF7F8FF) else Color(0xFF18243A)
    val muted = ink.copy(alpha = 0.66f)
    val base = if (darkMode) Color(0xFF101827) else Color(0xFFEAF1FB)
    val middle = if (darkMode) Color(0xFF263D60) else Color(0xFFB9CDE8)

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(base, middle, if (darkMode) Color(0xFF17243A) else Color(0xFFDCE8F7)))
            )
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Control Center", color = ink, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
                        Text("Your space. Your controls.", color = muted, fontSize = 14.sp)
                    }
                    Surface(onClick = { darkMode = !darkMode }, shape = CircleShape,
                        color = Color.White.copy(alpha = 0.13f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))) {
                        Icon(Icons.Default.Settings, contentDescription = "Toggle appearance", tint = ink,
                            modifier = Modifier.padding(13.dp).size(22.dp))
                    }
                }

                GlassPanel(glass) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CONNECTIVITY", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.weight(1f))
                        Text("QUICK CONTROLS", color = muted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    val controls = listOf(
                        Triple("Wi-Fi", Icons.Default.Wifi, true),
                        Triple("Bluetooth", Icons.Default.Bluetooth, false),
                        Triple("Airplane", Icons.Default.Flight, false),
                        Triple("Flashlight", Icons.Default.FlashlightOn, false)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        controls.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { item ->
                                    var active by remember { mutableStateOf(item.third) }
                                    ControlTile(item.first, item.second, active, accent, ink,
                                        Modifier.weight(1f)) { active = !active }
                                }
                            }
                        }
                    }
                }

                GlassPanel(glass) {
                    Text("DISPLAY & SOUND", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    var brightness by remember { mutableFloatStateOf(0.72f) }
                    var volume by remember { mutableFloatStateOf(0.55f) }
                    SliderLine("Brightness", brightness, { brightness = it }, accent, ink)
                    SliderLine("Volume", volume, { volume = it }, accent, ink)
                }

                GlassPanel(glass) {
                    Text("PERSONALIZE", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Glass intensity", color = ink, fontSize = 14.sp)
                    Slider(value = glass, onValueChange = { glass = it }, valueRange = 0.25f..0.85f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark appearance", color = ink, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = accent))
                    }
                    Text("Accent color", color = muted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(Color(0xFF9CCBFF), Color(0xFFC8A8FF), Color(0xFF8DE0C0), Color(0xFFFFC98F), Color(0xFFFF9EAE)).forEach { color ->
                            Surface(onClick = { accent = color }, shape = CircleShape, color = color,
                                border = BorderStroke(if (accent == color) 3.dp else 1.dp, Color.White.copy(alpha = 0.9f)),
                                modifier = Modifier.size(32.dp)) {}
                        }
                    }
                }
                Text("Visual preview • System shortcuts are still being developed", color = muted, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun ControlTile(label: String, icon: ImageVector, active: Boolean, accent: Color, ink: Color,
                        modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (active) accent.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.105f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (active) 0.42f else 0.20f))) {
        Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SliderLine(label: String, value: Float, onValueChange: (Float) -> Unit, accent: Color, ink: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = ink.copy(alpha = 0.82f), fontSize = 13.sp)
        Slider(value = value, onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
    }
}

@Composable
private fun GlassPanel(alpha: Float, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.075f + alpha * 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
