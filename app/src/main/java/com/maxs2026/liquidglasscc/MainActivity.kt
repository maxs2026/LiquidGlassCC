package com.maxs2026.liquidglasscc

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LiquidGlassApp() }
    }
}

@Composable
private fun LiquidGlassApp() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val lifecycleOwner = LocalLifecycleOwner.current
    var darkMode by remember { mutableStateOf(true) }
    var glass by remember { mutableFloatStateOf(0.52f) }
    var accent by remember { mutableStateOf(Color(0xFF9CCBFF)) }
    var brightness by remember { mutableFloatStateOf(readBrightness(context)) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
        audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionRequested = false
        if (Settings.System.canWrite(context)) brightness = readBrightness(context)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                brightness = readBrightness(context)
                permissionRequested = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ink = if (darkMode) Color(0xFFF7F8FF) else Color(0xFF18243A)
    val muted = ink.copy(alpha = 0.64f)
    val bg1 = if (darkMode) Color(0xFF0A1425) else Color(0xFFE9F1FC)
    val bg2 = if (darkMode) Color(0xFF29466C) else Color(0xFFB9CDE8)
    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(bg1, bg2, bg1)))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            val compact = maxHeight < 740.dp
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(if (compact) 54.dp else 62.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Control Center", color = ink, fontSize = if (compact) 25.sp else 28.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
                        Text("Liquid Glass", color = muted, fontSize = 13.sp)
                    }
                    RoundIcon(Icons.Default.Settings, "Appearance") { darkMode = !darkMode }
                }

                GlassPanel(glass, Modifier.weight(1.15f)) {
                    SectionLabel("CONNECTIVITY", muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                        QuickTile("Wi-Fi", Icons.Default.Wifi, true, accent, ink, Modifier.weight(1f)) {
                            openSettings(context, Settings.Panel.ACTION_WIFI)
                        }
                        QuickTile("Bluetooth", Icons.Default.Bluetooth, false, accent, ink, Modifier.weight(1f)) {
                            openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                        QuickTile("Airplane", Icons.Default.Flight, false, accent, ink, Modifier.weight(1f)) {
                            openSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                        }
                        QuickTile("Settings", Icons.Default.Settings, false, accent, ink, Modifier.weight(1f)) {
                            openSettings(context, Settings.ACTION_SETTINGS)
                        }
                    }
                }

                GlassPanel(glass, Modifier.weight(1.05f)) {
                    SectionLabel("DISPLAY & SOUND", muted)
                    SliderControl("Brightness", Icons.Default.Brightness6, brightness, accent, ink,
                        if (Settings.System.canWrite(context)) "SYSTEM" else "PERMISSION NEEDED") { value ->
                        if (Settings.System.canWrite(context)) {
                            brightness = value
                            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                                (value * 255).toInt().coerceIn(1, 255))
                        } else if (!permissionRequested) {
                            permissionRequested = true
                            permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")))
                        }
                    }
                    SliderControl("Media volume", Icons.Default.VolumeUp, volume, accent, ink, "SYSTEM") { value ->
                        volume = value
                        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).toInt(), 0)
                    }
                }

                GlassPanel(glass, Modifier.weight(0.88f)) {
                    SectionLabel("PERSONALIZE", muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark appearance", color = ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = accent))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Glass", color = ink, fontSize = 13.sp)
                        Slider(value = glass, onValueChange = { glass = it }, valueRange = 0.25f..0.8f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(Color(0xFF9CCBFF), Color(0xFFC8A8FF), Color(0xFF8DE0C0), Color(0xFFFFC98F), Color(0xFFFF9EAE)).forEach { c ->
                            Surface(onClick = { accent = c }, shape = CircleShape, color = c,
                                border = BorderStroke(if (accent == c) 3.dp else 1.dp, Color.White.copy(alpha = 0.8f)),
                                modifier = Modifier.size(27.dp)) {}
                        }
                        Spacer(Modifier.weight(1f))
                        Text("ACCENT", color = muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

private fun readBrightness(context: Context): Float =
    (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f).coerceIn(0.01f, 1f)

private fun openSettings(context: Context, action: String) {
    try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.padding(12.dp).size(21.dp))
    }
}

@Composable
private fun QuickTile(label: String, icon: ImageVector, active: Boolean, accent: Color, ink: Color,
                      modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(21.dp),
        color = if (active) accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.105f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (active) 0.42f else 0.20f))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SliderControl(label: String, icon: ImageVector, value: Float, accent: Color, ink: Color,
                          status: String, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ink.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, color = ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(status, color = if (status == "PERMISSION NEEDED") Color(0xFFFFC98F) else ink.copy(alpha = 0.48f),
                fontSize = 8.sp, letterSpacing = 0.8.sp)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0.01f..1f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)))
    }
}

@Composable
private fun GlassPanel(alpha: Float, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.075f + alpha * 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
