package com.maxs2026.liquidglasscc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
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
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(OverlayService.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false) && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        }
    }

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
        setContent { IOSControlCenter(isOverlay = false) }
    }
}

private val whiteGlass = Color(0xFFF7F8FC)
private val mutedGlass = Color(0xFFCFD5E2)
private val blueGlass = Color(0xFF42A5FF)
private val panelShape = RoundedCornerShape(30.dp)

@Composable
internal fun IOSControlCenter(isOverlay: Boolean) {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightness by remember { mutableFloatStateOf(readBrightness(ctx)) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
    var airplane by remember { mutableStateOf(false) }
    var flashlightOn by remember { mutableStateOf(false) }
    var askedPermission by remember { mutableStateOf(false) }
    val gesturePrefs = remember { ctx.getSharedPreferences(OverlayService.PREFS, Context.MODE_PRIVATE) }
    var edgeWidth by remember { mutableFloatStateOf(gesturePrefs.getInt(OverlayService.KEY_WIDTH, 24).toFloat()) }
    var swipeDistance by remember { mutableFloatStateOf(gesturePrefs.getInt(OverlayService.KEY_SWIPE, 64).toFloat()) }
    val batteryPercent = readBattery(ctx)

    if (!isOverlay) {
        AppSettingsScreen()
        return
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 760.dp
            val contentWidth = maxWidth * 0.82f
            val panelSize = (contentWidth - 12.dp) / 2
            if (!isOverlay) {
                Box(Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF090B12), Color(0xFF252B37), Color(0xFF111722), Color(0xFF080A10)))
                ))
                Box(Modifier.size(260.dp).offset(x = (-90).dp, y = 120.dp).blur(75.dp).background(Color(0xFF356C9C).copy(alpha = 0.34f), CircleShape))
                Box(Modifier.size(240.dp).align(Alignment.BottomEnd).offset(x = 80.dp, y = (-170).dp).blur(80.dp).background(Color(0xFF6A3D75).copy(alpha = 0.24f), CircleShape))
            }

            Column(
                Modifier.width(contentWidth).fillMaxHeight().align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = if (compact) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 15.dp)
            ) {
                Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(Icons.Default.Add, 39.dp) {
                        if (isOverlay) {
                            ctx.startService(Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_HIDE))
                        } else if (!Settings.canDrawOverlays(ctx)) {
                            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } else {
                            val overlayIntent = Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(overlayIntent) else ctx.startService(overlayIntent)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    RoundAction(Icons.Default.PowerSettingsNew, 39.dp) {
                        if (isOverlay) ctx.startService(Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_HIDE))
                        else openSettings(ctx, Settings.ACTION_SETTINGS)
                    }
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
                            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.42f), Color(0xFFB9DFFF).copy(alpha = 0.22f), Color(0xFF203B5A).copy(alpha = 0.38f))))
                            .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape), contentAlignment = Alignment.Center) {
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
                            color = Color.White.copy(alpha = 0.13f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.34f))
                        ) {
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF8BA7FF).copy(alpha = 0.58f), Color(0xFF435FAE).copy(alpha = 0.32f), Color(0xFF273D73).copy(alpha = 0.40f)))),
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
                            if (isOverlay) ctx.startService(Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_HIDE))
                            ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
                        } else {
                            if (isOverlay) ctx.startService(Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_HIDE))
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    ToolCircle(Icons.Default.CameraAlt, Modifier.weight(1f)) { openCamera(ctx) }
                    ToolCircle(Icons.Default.ScreenShare, Modifier.weight(1f), accent = Color(0xFF54C98B)) {
                        openSettings(ctx, Settings.ACTION_CAST_SETTINGS)
                    }
                    ToolCircle(Icons.Default.QrCodeScanner, Modifier.weight(1f), accent = Color(0xFFEC719A)) {
                        openSettings(ctx, Settings.ACTION_SETTINGS)
                    }
                }

                if (!isOverlay) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Swipe Gesture Settings", color = whiteGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Edge handle width: ${edgeWidth.toInt()} dp", color = mutedGlass, fontSize = 12.sp)
                            Slider(
                                value = edgeWidth,
                                onValueChange = {
                                    edgeWidth = it
                                    gesturePrefs.edit().putInt(OverlayService.KEY_WIDTH, it.toInt()).apply()
                                },
                                onValueChangeFinished = {
                                    val refresh = Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_REFRESH_SETTINGS)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(refresh) else ctx.startService(refresh)
                                },
                                valueRange = 12f..64f
                            )
                            Text("Swipe-down distance: ${swipeDistance.toInt()} dp", color = mutedGlass, fontSize = 12.sp)
                            Slider(
                                value = swipeDistance,
                                onValueChange = {
                                    swipeDistance = it
                                    gesturePrefs.edit().putInt(OverlayService.KEY_SWIPE, it.toInt()).apply()
                                },
                                onValueChangeFinished = {
                                    val refresh = Intent(ctx, OverlayService::class.java).setAction(OverlayService.ACTION_REFRESH_SETTINGS)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(refresh) else ctx.startService(refresh)
                                },
                                valueRange = 24f..160f
                            )
                            Text("Changes are saved automatically.", color = mutedGlass.copy(alpha = 0.70f), fontSize = 10.sp)
                        }
                    }
                }

                Text("LIQUID GLASS  •  CONTROL CENTER  •  v1.15", color = whiteGlass.copy(alpha = 0.30f), fontSize = 8.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, padding: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight().shadow(18.dp, panelShape),
        shape = panelShape,
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(
                    Color.White.copy(alpha = 0.30f),
                    Color(0xFFBBDFFF).copy(alpha = 0.16f),
                    Color(0xFF7898B9).copy(alpha = 0.10f),
                    Color(0xFF071426).copy(alpha = 0.34f),
                    Color.White.copy(alpha = 0.12f)
                )), panelShape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.11f), Color.Transparent, Color(0xFF06101E).copy(alpha = 0.12f))), panelShape)
                .padding(padding),
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
            .background(if (active) Brush.linearGradient(listOf(Color(0xFF79C9FF), Color(0xFF268BE5), Color(0xFF12518F))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.43f), Color(0xFFBBDFFF).copy(alpha = 0.23f), Color(0xFF182D47).copy(alpha = 0.42f))))
            .border(1.dp, Color.White.copy(alpha = 0.43f), CircleShape), contentAlignment = Alignment.Center) {
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
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f))
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.36f), Color(0xFFB9DFFF).copy(alpha = 0.20f), Color(0xFF1B3048).copy(alpha = 0.34f))), CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.10f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
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
                        else listOf(accent.copy(alpha = 0.43f), Color.White.copy(alpha = 0.25f), Color(0xFF17263A).copy(alpha = 0.48f))
                    ))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.17f), Color.Transparent, Color.Black.copy(alpha = 0.12f)))),
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
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.36f), Color(0xFFBBD8F8).copy(alpha = 0.20f), Color(0xFF263B56).copy(alpha = 0.42f))))
            .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(32.dp))
            .pointerInput(Unit) { detectTapGestures { tap -> onValueChange((1f - tap.y / size.height).coerceIn(0.01f, 1f)) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); onValueChange((1f - change.position.y / size.height).coerceIn(0.01f, 1f)) } },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(value.coerceIn(0.01f, 1f)).clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F8FF), Color(0xFFBBDFFF), accent.copy(alpha = 0.94f)))))
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


@Composable
private fun AppSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(OverlayService.PREFS, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var showAppearance by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var writeGranted by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var notificationAccessGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val notificationGranted = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF2F1F7)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (showAppearance) {
                    AppearanceSettingsScreen(onBack = { showAppearance = false })
                } else {
                    Text("Control Center", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Switch(checked = enabled, onCheckedChange = { value ->
                                enabled = value
                                prefs.edit().putBoolean("enabled", value).apply()
                                if (value) {
                                    if (!Settings.canDrawOverlays(context)) {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    } else {
                                        val i = Intent(context, OverlayService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
                                    }
                                } else {
                                    context.stopService(Intent(context, OverlayService::class.java))
                                }
                            })
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (!Settings.canDrawOverlays(context)) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } else {
                                val openIntent = Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(openIntent) else context.startService(openIntent)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1677E8)
                    ) {
                        Text("Open Control Center now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
                    }
                    Text("PERMISSIONS", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Column {
                            PermissionSettingRow(Icons.Default.Layers, "Display over other apps", "Show the panel over any app", overlayGranted) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            PermissionSettingRow(Icons.Default.Tune, "Change system settings", "Brightness, timeout, rotation", writeGranted) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            PermissionSettingRow(Icons.Default.Notifications, "Notification access", "Music card and playback", notificationAccessGranted) {
                                openSettings(context, "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            }
                            PermissionSettingRow(Icons.Default.NotificationsActive, "Allow notifications", "Background status and results", notificationGranted) {
                                if (Build.VERSION.SDK_INT >= 33) context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            PermissionSettingRow(Icons.Default.DoNotDisturbOn, "Do Not Disturb access", "Do Not Disturb and ringer · Optional", false) {
                                openSettings(context, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            }
                            PermissionSettingRow(Icons.Default.Bluetooth, "Nearby devices", "Turn Bluetooth on · Optional", false) {
                                openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                            }
                        }
                    }
                    if (!overlayGranted || !writeGranted) {
                        Text("Turn on the required permissions to enable Control Center.", color = Color(0xFFE39A25), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Text("GENERAL", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Column {
                            GeneralSettingRow(Icons.Default.WaterDrop, "Appearance") { showAppearance = true }
                            GeneralSettingRow(Icons.Default.Settings, "Customize Controls") { showAppearance = true }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSettingRow(icon: ImageVector, title: String, subtitle: String, granted: Boolean, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF3479E8)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(23.dp)) }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = Color.Black, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Switch(checked = granted, onCheckedChange = { onClick() })
        }
        Divider(color = Color(0xFFE5E5E5), modifier = Modifier.padding(start = 72.dp))
    }
}

@Composable
private fun GeneralSettingRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF8B55D9), modifier = Modifier.size(28.dp))
        Text(title, color = Color.Black, fontSize = 17.sp, modifier = Modifier.padding(start = 18.dp))
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}

@Composable
private fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(OverlayService.PREFS, Context.MODE_PRIVATE) }
    var location by remember { mutableStateOf(prefs.getString(OverlayService.KEY_LOCATION, "TOP_RIGHT") ?: "TOP_RIGHT") }
    var edgeWidth by remember { mutableFloatStateOf(prefs.getInt(OverlayService.KEY_WIDTH, 24).toFloat()) }
    var edgeHeight by remember { mutableFloatStateOf(prefs.getInt(OverlayService.KEY_HEIGHT, 142).toFloat()) }
    var hue by remember { mutableFloatStateOf(prefs.getInt(OverlayService.KEY_HUE, 210).toFloat()) }
    MaterialTheme(colorScheme = lightColorScheme()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF1684EA)) }
                Text("Appearance", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("CHANGE THE LOCATION", color = Color.Gray, fontSize = 13.sp)
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("TOP_RIGHT", "TOP", "BOTTOM", "LEFT", "RIGHT").forEach { option ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable {
                                location = option
                                prefs.edit().putString(OverlayService.KEY_LOCATION, option).apply()
                                refreshOverlayService(context)
                            }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Smartphone, null, tint = if (location == option) Color(0xFF087FF5) else Color.DarkGray, modifier = Modifier.size(42.dp))
                                Text(if (option == "TOP_RIGHT") "Top right" else option.lowercase().replaceFirstChar { it.uppercase() }, color = if (location == option) Color(0xFF087FF5) else Color.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Text("You can change where the swipe gesture starts.", color = Color.Gray, fontSize = 13.sp)
            Text("SIZE", color = Color.Gray, fontSize = 13.sp)
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Width · ${edgeWidth.toInt()} dp", color = Color.Gray)
                    Slider(value = edgeWidth, onValueChange = { edgeWidth = it; prefs.edit().putInt(OverlayService.KEY_WIDTH, it.toInt()).apply() }, onValueChangeFinished = { refreshOverlayService(context) }, valueRange = 12f..64f)
                    Divider()
                    Text("Height · ${edgeHeight.toInt()} dp", color = Color.Gray)
                    Slider(value = edgeHeight, onValueChange = { edgeHeight = it; prefs.edit().putInt(OverlayService.KEY_HEIGHT, it.toInt()).apply() }, onValueChangeFinished = { refreshOverlayService(context) }, valueRange = 60f..320f)
                }
            }
            Text("COLOR", color = Color.Gray, fontSize = 13.sp)
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(18.dp)) {
                    Text("Choose color", color = Color.Gray)
                    Slider(value = hue, onValueChange = { hue = it; prefs.edit().putInt(OverlayService.KEY_HUE, it.toInt()).apply() }, onValueChangeFinished = { refreshOverlayService(context) }, valueRange = 0f..360f)
                    Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp)).background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 1f)))))
                }
            }
        }
    }
}

private fun refreshOverlayService(context: Context) {
    if (!prefsEnabled(context)) return
    val intent = Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_REFRESH_SETTINGS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    return enabled.split(":").any { it.contains(context.packageName) }
}

private fun prefsEnabled(context: Context): Boolean =
    context.getSharedPreferences(OverlayService.PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)
