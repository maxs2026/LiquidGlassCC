package com.maxs2026.liquidglasscc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        setContent { IOSControlCenter() }
    }
}

private val glassWhite = Color(0xFFF7F9FF)
private val glassMuted = Color(0xFFB8C4D8)
private val glassBlue = Color(0xFF70B9FF)

@Composable
private fun IOSControlCenter() {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightness by remember { mutableFloatStateOf(readBrightness(ctx)) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
    var focus by remember { mutableStateOf(false) }
    var rotation by remember { mutableStateOf(false) }
    var airplane by remember { mutableStateOf(false) }
    var flashlightOn by remember { mutableStateOf(false) }
    val batteryPercent = remember { readBattery(ctx) }
    var askedPermission by remember { mutableStateOf(false) }
    val flashlightPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) flashlightOn = setTorch(ctx, true)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        askedPermission = false
        if (Settings.System.canWrite(ctx)) brightness = readBrightness(ctx)
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF080D19), Color(0xFF172B49), Color(0xFF253D5D), Color(0xFF101A2B)),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
        ) {
            val compact = maxHeight < 730.dp
            Column(
                Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = if (compact) 12.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 11.dp else 15.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Control Center", color = glassWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                        Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()), color = glassMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()), color = glassWhite, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text("$batteryPercent%  •  4G", color = glassMuted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    RoundAction(Icons.Default.PowerSettingsNew, glassWhite, if (compact) 39.dp else 43.dp) {
                        openSettings(ctx, Settings.ACTION_SETTINGS)
                    }
                }

                Row(Modifier.weight(1.06f), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    ConnectivityPanel(
                        Modifier.weight(1.18f),
                        airplane = airplane,
                        onAirplane = {
                            airplane = !airplane
                            openSettings(ctx, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                        },
                        onWifi = { openSettings(ctx, Settings.Panel.ACTION_WIFI) },
                        onCellular = { openSettings(ctx, Settings.ACTION_DATA_ROAMING_SETTINGS) },
                        onBluetooth = { openSettings(ctx, Settings.ACTION_BLUETOOTH_SETTINGS) }
                    )
                    GlassPanel(Modifier.weight(0.82f), title = "MEDIA") {
                        Box(
                            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(19.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF45668F), Color(0xFF182C4A)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(38.dp))
                        }
                        Text("Nothing Playing", color = glassWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("Music controls", color = glassMuted, fontSize = 9.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipPrevious, null, tint = glassMuted, modifier = Modifier.size(19.dp))
                            Icon(Icons.Default.PlayArrow, null, tint = glassWhite, modifier = Modifier.size(27.dp))
                            Icon(Icons.Default.SkipNext, null, tint = glassMuted, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                Row(Modifier.weight(1.12f), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    GlassPanel(Modifier.weight(1.16f), title = "QUICK CONTROLS") {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniControl("Focus", Icons.Default.DarkMode, focus, Modifier.weight(1f)) {
                                focus = !focus
                                openSettings(ctx, Settings.ACTION_SOUND_SETTINGS)
                            }
                            MiniControl("Rotation", Icons.Default.ScreenLockRotation, rotation, Modifier.weight(1f)) {
                                rotation = !rotation
                                openSettings(ctx, Settings.ACTION_DISPLAY_SETTINGS)
                            }
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniControl("Do Not Disturb", Icons.Default.NotificationsOff, false, Modifier.weight(1f)) {
                                openSettings(ctx, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            }
                            MiniControl(if (flashlightOn) "Torch On" else "Torch", if (flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff, flashlightOn, Modifier.weight(1f)) {
                                if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    flashlightOn = setTorch(ctx, !flashlightOn)
                                } else flashlightPermission.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                    }
                    Column(Modifier.weight(0.62f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalSystemSlider(brightness, glassBlue, glassWhite, Icons.Default.Brightness6, Modifier.weight(1f)) { value ->
                            if (Settings.System.canWrite(ctx)) {
                                brightness = value
                                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt().coerceIn(1, 255))
                            } else if (!askedPermission) {
                                askedPermission = true
                                permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                            }
                        }
                    }
                    Column(Modifier.weight(0.62f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalSystemSlider(volume, glassBlue, glassWhite, Icons.Default.VolumeUp, Modifier.weight(1f)) { value ->
                            volume = value
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShortcutButton(Icons.Default.FlashlightOn, "Flashlight", Modifier.weight(1f)) {
                        if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            flashlightOn = setTorch(ctx, !flashlightOn)
                        } else flashlightPermission.launch(android.Manifest.permission.CAMERA)
                    }
                    ShortcutButton(Icons.Default.Calculate, "Calculator", Modifier.weight(1f)) { openCalculator(ctx) }
                    ShortcutButton(Icons.Default.QrCodeScanner, "Scanner", Modifier.weight(1f)) { openSettings(ctx, Settings.ACTION_SETTINGS) }
                    ShortcutButton(Icons.Default.Timer, "Timer", Modifier.weight(1f)) { openSettings(ctx, Settings.ACTION_DATE_SETTINGS) }
                }
                Text("LIQUID GLASS  •  CONTROL CENTER  •  v1.3", color = glassWhite.copy(alpha = 0.34f), fontSize = 8.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxHeight().shadow(18.dp, RoundedCornerShape(29.dp)),
        shape = RoundedCornerShape(29.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color(0xFFB8D8FF).copy(alpha = 0.105f),
                            Color(0xFF0D1C34).copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.075f)
                        )
                    ),
                    RoundedCornerShape(29.dp)
                )
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = glassMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.35.sp)
            content()
        }
    }
}

@Composable
private fun ConnectivityPanel(
    modifier: Modifier = Modifier,
    airplane: Boolean,
    onAirplane: () -> Unit,
    onWifi: () -> Unit,
    onCellular: () -> Unit,
    onBluetooth: () -> Unit
) {
    GlassPanel(modifier, title = "CONNECTIVITY") {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ConnectivityCircle("Wi-Fi", Icons.Default.Wifi, false, Modifier.weight(1f), onWifi)
                ConnectivityCircle("Data", Icons.Default.SignalCellularAlt, false, Modifier.weight(1f), onCellular)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ConnectivityCircle("Airplane", Icons.Default.Flight, airplane, Modifier.weight(1f), onAirplane)
                ConnectivityCircle("Bluetooth", Icons.Default.Bluetooth, false, Modifier.weight(1f), onBluetooth)
            }
        }
    }
}

@Composable
private fun ConnectivityCircle(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(54.dp)
                .shadow(7.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (active) listOf(Color(0xFF70BFFF), Color(0xFF1767B2))
                        else listOf(Color.White.copy(alpha = 0.27f), Color(0xFFB9D8FF).copy(alpha = 0.12f), Color(0xFF0B1930).copy(alpha = 0.30f))
                    )
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = glassWhite, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = glassWhite, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun MiniControl(label: String, icon: ImageVector, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (active) 0.34f else 0.22f))
    ) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = if (active)
                        listOf(Color(0xFF64B8FF).copy(alpha = 0.82f), Color(0xFF1767B2).copy(alpha = 0.62f), Color.White.copy(alpha = 0.12f))
                    else
                        listOf(Color.White.copy(alpha = 0.22f), Color(0xFFB9D8FF).copy(alpha = 0.10f), Color(0xFF0B1930).copy(alpha = 0.20f))
                ),
                RoundedCornerShape(20.dp)
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // iOS-inspired circular icon medallion inside each control tile
            Box(
                Modifier.size(43.dp)
                    .shadow(5.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (active)
                                listOf(Color.White.copy(alpha = 0.30f), Color(0xFF73BFFF).copy(alpha = 0.32f), Color(0xFF174A80).copy(alpha = 0.42f))
                            else
                                listOf(Color.White.copy(alpha = 0.25f), Color(0xFFB9D8FF).copy(alpha = 0.12f), Color(0xFF0B1930).copy(alpha = 0.28f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = glassWhite, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, color = glassWhite, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.4f)) }
    }
}

@Composable
private fun ShortcutButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.15f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), modifier = Modifier.size(49.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = glassWhite, modifier = Modifier.size(21.dp)) }
        }
        Text(label, color = glassMuted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun VerticalSystemSlider(value: Float, accent: Color, white: Color, icon: ImageVector, modifier: Modifier = Modifier, onValueChange: (Float) -> Unit) {
    // Narrow iOS-style capsule: the rail is deliberately slim, with its glyph tucked inside.
    Box(
        modifier = modifier
            .width(58.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color(0xFFBBD8F8).copy(alpha = 0.15f), Color(0xFF0B1A30).copy(alpha = 0.34f))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)), RoundedCornerShape(32.dp))
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    onValueChange((1f - tap.y / size.height).coerceIn(0.01f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onValueChange((1f - change.position.y / size.height).coerceIn(0.01f, 1f))
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier.fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0.01f, 1f))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.98f), Color(0xFFEAF5FF).copy(alpha = 0.96f), accent.copy(alpha = 0.88f))
                    )
                )
        )
        Icon(
            icon,
            contentDescription = if (icon == Icons.Default.Brightness6) "Brightness" else "Volume",
            tint = if (value > 0.48f) Color(0xFF23415F) else white,
            modifier = Modifier.padding(bottom = 13.dp).size(18.dp)
        )
    }
}

private fun readBrightness(context: Context): Float =
    (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f).coerceIn(0.01f, 1f)

private fun readBattery(context: Context): Int {
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
}

private fun setTorch(context: Context, enabled: Boolean): Boolean {
    return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = manager.cameraIdList.firstOrNull { cameraId ->
            manager.getCameraCharacteristics(cameraId).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        manager.setTorchMode(id, enabled)
        enabled
    } catch (_: Exception) { false }
}

private fun openFlashlightSettings(context: Context) {
    try { context.startActivity(Intent("android.settings.FLASHLIGHT_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { openSettings(context, Settings.ACTION_SETTINGS) }
}

private fun openCalculator(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { openSettings(context, Settings.ACTION_SETTINGS) }
}

private fun openSettings(context: Context, action: String) {
    try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
