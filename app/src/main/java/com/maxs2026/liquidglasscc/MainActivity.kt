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

@Composable
private fun IOSControlCenter() {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val white = Color(0xFFF8FAFF)
    val muted = white.copy(alpha = 0.70f)
    val blue = Color(0xFF69B7FF)
    var brightness by remember { mutableFloatStateOf(readBrightness(ctx)) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
    var focus by remember { mutableStateOf(false) }
    var rotation by remember { mutableStateOf(false) }
    var airplane by remember { mutableStateOf(false) }
    var askedPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        askedPermission = false
        if (Settings.System.canWrite(ctx)) brightness = readBrightness(ctx)
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(Color(0xFF050A16), Color(0xFF102747), Color(0xFF263F60), Color(0xFF091426))
                )
            ).padding(start = 15.dp, end = 15.dp, top = 12.dp, bottom = 12.dp)
        ) {
            val compact = maxHeight < 700.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp)) {
                Row(Modifier.fillMaxWidth().height(if (compact) 42.dp else 50.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()), color = white, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("•••  4G  ▮▮  ▰", color = muted, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    RoundAction(Icons.Default.PowerSettingsNew, white, 34.dp) { openSettings(ctx, Settings.ACTION_SETTINGS) }
                }

                Row(Modifier.weight(1.45f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPanel(Modifier.weight(1.08f)) {
                        Text("Connectivity", color = muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ControlTile("Airplane", Icons.Default.Flight, airplane, blue, white, Modifier.weight(1f)) {
                                airplane = !airplane
                                openSettings(ctx, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                            }
                            ControlTile("Wi-Fi", Icons.Default.Wifi, true, blue, white, Modifier.weight(1f)) {
                                openSettings(ctx, Settings.Panel.ACTION_WIFI)
                            }
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            ControlTile("Bluetooth", Icons.Default.Bluetooth, false, blue, white, Modifier.weight(1f)) {
                                openSettings(ctx, Settings.ACTION_BLUETOOTH_SETTINGS)
                            }
                            ControlTile("Mobile data", Icons.Default.SignalCellularAlt, false, blue, white, Modifier.weight(1f)) {
                                openSettings(ctx, Settings.ACTION_DATA_ROAMING_SETTINGS)
                            }
                        }
                    }
                    GlassPanel(Modifier.weight(0.92f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Now Playing", color = white, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.MoreHoriz, null, tint = muted, modifier = Modifier.size(19.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF647DA6), Color(0xFF293C5D)))), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = white, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Not Playing", color = white, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("Media player", color = muted, fontSize = 9.sp)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipPrevious, null, tint = muted, modifier = Modifier.size(21.dp))
                            Icon(Icons.Default.PlayArrow, null, tint = white, modifier = Modifier.size(29.dp))
                            Icon(Icons.Default.SkipNext, null, tint = muted, modifier = Modifier.size(21.dp))
                        }
                    }
                }

                Row(Modifier.weight(0.88f), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundAction(Icons.Default.ScreenLockRotation, if (rotation) blue else white, 57.dp) {
                        rotation = !rotation
                        openSettings(ctx, Settings.ACTION_DISPLAY_SETTINGS)
                    }
                    RoundAction(Icons.Default.NotificationsOff, white, 57.dp) {
                        openSettings(ctx, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    }
                    VerticalSystemSlider(brightness, blue, white, Icons.Default.Brightness6, Modifier.weight(1f)) { value ->
                        if (Settings.System.canWrite(ctx)) {
                            brightness = value
                            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt().coerceIn(1, 255))
                        } else if (!askedPermission) {
                            askedPermission = true
                            permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                        }
                    }
                    VerticalSystemSlider(volume, blue, white, Icons.Default.VolumeUp, Modifier.weight(1f)) { value ->
                        volume = value
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
                    }
                }

                Row(Modifier.weight(0.48f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = { focus = !focus; openSettings(ctx, Settings.ACTION_SOUND_SETTINGS) },
                        shape = RoundedCornerShape(30.dp), color = if (focus) blue.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, null, tint = white, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(if (focus) "Focus On" else "Focus", color = white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(Modifier.weight(0.78f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LongControl("Flashlight", Icons.Default.FlashlightOn, Modifier.weight(1.25f)) { openSettings(ctx, Settings.ACTION_SETTINGS) }
                    RoundAction(Icons.Default.Calculator, white, 51.dp) { openCalculator(ctx) }
                    RoundAction(Icons.Default.QrCodeScanner, white, 51.dp) { openSettings(ctx, Settings.ACTION_SETTINGS) }
                    RoundAction(Icons.Default.Timer, white, 51.dp) { openSettings(ctx, Settings.ACTION_DATE_SETTINGS) }
                }
                Text("LIQUID GLASS  •  CONTROL CENTER", color = white.copy(alpha = 0.35f), fontSize = 8.sp, letterSpacing = 1.7.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp), color = Color.White.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))) {
        Column(Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun ControlTile(label: String, icon: ImageVector, active: Boolean, accent: Color, ink: Color,
                        modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(17.dp),
        color = if (active) Color(0xFF1678D5).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.19f)), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.38f)) }
    }
}

@Composable
private fun LongControl(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(51.dp), shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.15f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VerticalSystemSlider(value: Float, accent: Color, white: Color, icon: ImageVector,
                                 modifier: Modifier = Modifier, onValueChange: (Float) -> Unit) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.17f))
            .pointerInput(Unit) {
                detectTapGestures { tap -> onValueChange((1f - tap.y / size.height).coerceIn(0.01f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onValueChange((1f - change.position.y / size.height).coerceIn(0.01f, 1f))
                }
            }, contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(value.coerceIn(0.01f, 1f))
                .clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color.White, accent.copy(alpha = 0.95f)))))
        }
        Spacer(Modifier.height(7.dp))
        Icon(icon, null, tint = white, modifier = Modifier.size(19.dp))
    }
}

private fun readBrightness(context: Context): Float =
    (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f).coerceIn(0.01f, 1f)

private fun openCalculator(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { openSettings(context, Settings.ACTION_SETTINGS) }
}

private fun openSettings(context: Context, action: String) {
    try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
