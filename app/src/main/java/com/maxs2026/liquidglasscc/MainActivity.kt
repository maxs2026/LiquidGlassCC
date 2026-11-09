package com.maxs2026.liquidglasscc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        setContent { IOSControlCenter() }
    }
}

private val whiteGlass = Color(0xFFF7F8FC)
private val mutedGlass = Color(0xFFCFD5E2)
private val blueGlass = Color(0xFF42A5FF)
private val panelShape = RoundedCornerShape(30.dp)

@Composable
private fun IOSControlCenter() {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightness by remember { mutableFloatStateOf(readBrightness(ctx)) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
    var airplane by remember { mutableStateOf(false) }
    var flashlightOn by remember { mutableStateOf(false) }
    var askedPermission by remember { mutableStateOf(false) }
    val batteryPercent = readBattery(ctx)
    val flashlightPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) flashlightOn = setTorch(ctx, true)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        askedPermission = false
        if (Settings.System.canWrite(ctx)) brightness = readBrightness(ctx)
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 760.dp
            val contentWidth = maxWidth * 0.82f
            val panelSize = (contentWidth - 12.dp) / 2
            Box(Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF090B12), Color(0xFF252B37), Color(0xFF111722), Color(0xFF080A10)))
            ))
            Box(Modifier.size(260.dp).offset(x = (-90).dp, y = 120.dp).blur(75.dp).background(Color(0xFF356C9C).copy(alpha = 0.34f), CircleShape))
            Box(Modifier.size(240.dp).align(Alignment.BottomEnd).offset(x = 80.dp, y = (-170).dp).blur(80.dp).background(Color(0xFF6A3D75).copy(alpha = 0.24f), CircleShape))

            Column(
                Modifier.width(contentWidth).fillMaxHeight().align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = if (compact) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 15.dp)
            ) {
                Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(Icons.Default.Add, 39.dp) { openSettings(ctx, Settings.ACTION_SETTINGS) }
                    Spacer(Modifier.weight(1f))
                    RoundAction(Icons.Default.PowerSettingsNew, 39.dp) { openSettings(ctx, Settings.ACTION_SETTINGS) }
                }

                Row(Modifier.fillMaxWidth().height(panelSize), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConnectivityPanel(
                        Modifier.weight(1f),
                        airplane = airplane,
                        onAirplane = {
                            airplane = !airplane
                            openSettings(ctx, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                        },
                        onWifi = { openSettings(ctx, Settings.Panel.ACTION_WIFI) },
                        onCellular = { openSettings(ctx, Settings.ACTION_DATA_USAGE_SETTINGS) },
                        onBluetooth = { openSettings(ctx, Settings.ACTION_BLUETOOTH_SETTINGS) }
                    )
                    GlassPanel(Modifier.weight(1f), padding = 12.dp) {
                        Box(Modifier.size(54.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color(0xFF9BCBFF).copy(alpha = 0.15f), Color(0xFF14243A).copy(alpha = 0.34f))))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, tint = whiteGlass, modifier = Modifier.size(23.dp))
                        }
                        Text("Not Playing", color = whiteGlass, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipPrevious, null, tint = mutedGlass, modifier = Modifier.size(20.dp))
                            Icon(Icons.Default.PlayArrow, null, tint = whiteGlass, modifier = Modifier.size(23.dp))
                            Icon(Icons.Default.SkipNext, null, tint = mutedGlass, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().height(panelSize),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().height(62.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundUtilityCircle(Icons.Default.ScreenRotation) {
                                openSettings(ctx, Settings.ACTION_DISPLAY_SETTINGS)
                            }
                            RoundUtilityCircle(Icons.Default.Notifications) {
                                openAppNotificationSettings(ctx)
                            }
                        }
                        Surface(
                            onClick = { openSettings(ctx, Settings.ACTION_SOUND_SETTINGS) },
                            modifier = Modifier.fillMaxWidth().height(68.dp),
                            shape = RoundedCornerShape(36.dp),
                            color = Color.White.copy(alpha = 0.17f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                        ) {
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape)
                                        .background(Color(0xFF718FF0).copy(alpha = 0.34f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bedtime, null, tint = whiteGlass, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text("Focus", color = whiteGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Do Not Disturb", color = mutedGlass.copy(alpha = 0.72f), fontSize = 9.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ExpandMore, null, tint = mutedGlass, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    VerticalSystemSlider(brightness, blueGlass, whiteGlass, Icons.Default.WbSunny, Modifier.width(66.dp).fillMaxHeight()) { value ->
                        if (Settings.System.canWrite(ctx)) {
                            brightness = value
                            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt().coerceIn(1, 255))
                        } else if (!askedPermission) {
                            askedPermission = true
                            permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName)))
                        }
                    }
                    VerticalSystemSlider(volume, blueGlass, whiteGlass, Icons.Default.VolumeUp, Modifier.width(66.dp).fillMaxHeight()) { value ->
                        volume = value
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolCircle(Icons.Default.FlashlightOn, Modifier.weight(1f), active = flashlightOn) {
                        if (ctx.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            flashlightOn = setTorch(ctx, !flashlightOn)
                        } else flashlightPermission.launch(android.Manifest.permission.CAMERA)
                    }
                    ToolCircle(Icons.Default.CameraAlt, Modifier.weight(1f)) { openCamera(ctx) }
                    ToolCircle(Icons.Default.ScreenShare, Modifier.weight(1f), accent = Color(0xFF54C98B)) {
                        openSettings(ctx, Settings.ACTION_CAST_SETTINGS)
                    }
                    ToolCircle(Icons.Default.QrCodeScanner, Modifier.weight(1f), accent = Color(0xFFEC719A)) {
                        openSettings(ctx, Settings.ACTION_SETTINGS)
                    }
                }

                Text("LIQUID GLASS  •  CONTROL CENTER  •  v1.14", color = whiteGlass.copy(alpha = 0.30f), fontSize = 8.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, padding: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight().shadow(18.dp, panelShape),
        shape = panelShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
    ) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color(0xFFB8D8FF).copy(alpha = 0.10f), Color(0xFF0D1C34).copy(alpha = 0.30f), Color.White.copy(alpha = 0.07f))),
                panelShape
            ).padding(padding),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content
        )
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
    GlassPanel(modifier, padding = 11.dp) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ConnectivityCircle(Icons.Default.Wifi, Modifier.weight(1f), onWifi)
                ConnectivityCircle(Icons.Default.SignalCellularAlt, Modifier.weight(1f), onCellular)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ConnectivityCircle(Icons.Default.Flight, Modifier.weight(1f), onAirplane, active = airplane)
                ConnectivityCircle(Icons.Default.Bluetooth, Modifier.weight(1f), onBluetooth)
            }
        }
    }
}

@Composable
private fun ConnectivityCircle(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit, active: Boolean = false) {
    Box(modifier.fillMaxHeight().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(54.dp).shadow(7.dp, CircleShape).clip(CircleShape)
            .background(if (active) Brush.linearGradient(listOf(Color(0xFF71C5FF), Color(0xFF1767B2))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color(0xFFB9D8FF).copy(alpha = 0.13f), Color(0xFF0B1930).copy(alpha = 0.32f))))
            .border(1.dp, Color.White.copy(alpha = 0.31f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = whiteGlass, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, size: Dp, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = whiteGlass, modifier = Modifier.size(size * 0.42f)) }
    }
}

@Composable
private fun RoundUtilityCircle(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(62.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = whiteGlass, modifier = Modifier.size(29.dp))
        }
    }
}

@Composable
private fun RoundUtility(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(62.dp), shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.15f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = whiteGlass, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = whiteGlass, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ToolCircle(icon: ImageVector, modifier: Modifier = Modifier, active: Boolean = false, accent: Color = Color(0xFF76BFFF), onClick: () -> Unit) {
    // Keep the original 72dp row slot so removing captions does not shift the
    // controls or the footer vertically. The visible control matches the
    // 54dp connectivity circles.
    Box(modifier.height(72.dp), contentAlignment = Alignment.TopCenter) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(62.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
        ) {
            Box(
                Modifier.shadow(9.dp, CircleShape).clip(CircleShape)
                    .background(Brush.linearGradient(
                        if (active) listOf(accent, Color(0xFF1767B2))
                        else listOf(accent.copy(alpha = 0.34f), Color.White.copy(alpha = 0.13f), Color(0xFF111B2A).copy(alpha = 0.36f))
                    )),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = whiteGlass, modifier = Modifier.size(29.dp))
            }
        }
    }
}

@Composable
private fun VerticalSystemSlider(value: Float, accent: Color, white: Color, icon: ImageVector, modifier: Modifier = Modifier, onValueChange: (Float) -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color(0xFFBBD8F8).copy(alpha = 0.14f), Color(0xFF0B1A30).copy(alpha = 0.38f))))
            .border(1.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(32.dp))
            .pointerInput(Unit) { detectTapGestures { tap -> onValueChange((1f - tap.y / size.height).coerceIn(0.01f, 1f)) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); onValueChange((1f - change.position.y / size.height).coerceIn(0.01f, 1f)) } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(value.coerceIn(0.01f, 1f)).clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.98f), Color(0xFFEAF5FF).copy(alpha = 0.96f), accent.copy(alpha = 0.88f)))))
        Icon(icon, null, tint = if (icon == Icons.Default.WbSunny) Color(0xFFFFC928) else Color(0xFF16B8D4), modifier = Modifier.padding(bottom = 28.dp).size(18.dp))
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

private fun openAppNotificationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        openSettings(context, Settings.ACTION_SETTINGS)
    }
}

private fun openCalculator(context: Context) {
    try { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { openSettings(context, Settings.ACTION_SETTINGS) }
}

private fun openCamera(context: Context) {
    try { context.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { openSettings(context, Settings.ACTION_SETTINGS) }
}

private fun openSettings(context: Context, action: String) {
    try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
