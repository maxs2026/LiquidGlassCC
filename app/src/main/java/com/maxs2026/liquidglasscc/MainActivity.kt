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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { LiquidGlassApp() } }
}

@Composable private fun LiquidGlassApp() {
 val ctx= LocalContext.current
 val audio= remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
 val white=Color(0xFFF8F8FC); val muted=white.copy(alpha=.65f); val blue=Color(0xFF91C5FF)
 var bright by remember { mutableFloatStateOf(readBrightness(ctx)) }
 var vol by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()/audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
 var focus by remember { mutableStateOf(false) }; var dark by remember { mutableStateOf(true) }
 var asked by remember { mutableStateOf(false) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { asked=false; if(Settings.System.canWrite(ctx)) bright=readBrightness(ctx) }
 MaterialTheme(colorScheme=darkColorScheme()) {
  BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF090D18),Color(0xFF263B59),Color(0xFF101827)))).padding(horizontal=16.dp, vertical=12.dp)) {
   val tight=maxHeight<710.dp
   Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(if(tight)8.dp else 11.dp)) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     Column(Modifier.weight(1f)) {
      Text("Control Center",color=white,fontSize=27.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.8).sp)
      Text("Monday • Your controls",color=muted,fontSize=12.sp)
     }
     CircleButton(Icons.Default.Settings,white){ open(ctx,Settings.ACTION_SETTINGS) }
    }
    GlassBox(Modifier.weight(1.08f)) {
     Text("Connectivity",color=muted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
     Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Tile("Wi-Fi",Icons.Default.Wifi,true,blue,white,Modifier.weight(1f)){open(ctx,Settings.Panel.ACTION_WIFI)}
      Tile("Bluetooth",Icons.Default.Bluetooth,false,blue,white,Modifier.weight(1f)){open(ctx,Settings.ACTION_BLUETOOTH_SETTINGS)}
     }
     Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Tile("Airplane",Icons.Default.Flight,false,blue,white,Modifier.weight(1f)){open(ctx,Settings.ACTION_AIRPLANE_MODE_SETTINGS)}
      Tile("Flashlight",Icons.Default.FlashlightOn,false,blue,white,Modifier.weight(1f)){open(ctx,Settings.ACTION_SETTINGS)}
     }
    }
    Row(Modifier.weight(.34f),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
     Tile("Focus",Icons.Default.DoNotDisturbOn,focus,blue,white,Modifier.weight(1f)){focus=!focus}
     Tile("Rotation",Icons.Default.ScreenLockRotation,false,blue,white,Modifier.weight(1f)){open(ctx,Settings.ACTION_DISPLAY_SETTINGS)}
    }
    GlassBox(Modifier.weight(.65f)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Box(Modifier.size(40.dp).background(Color.White.copy(alpha=.13f),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=white)}
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)){Text("Now Playing",color=white,fontWeight=FontWeight.SemiBold);Text("Media controls",color=muted,fontSize=11.sp)}
      Text("•••",color=muted,fontSize=18.sp)
     }
     Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.VolumeUp,null,tint=muted,modifier=Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Slider(vol.coerceIn(.01f,1f),{vol=it;audio.setStreamVolume(AudioManager.STREAM_MUSIC,(it*audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(),0)},colors=SliderDefaults.colors(thumbColor=white,activeTrackColor=blue,inactiveTrackColor=white.copy(alpha=.18f)))}
    }
    GlassBox(Modifier.weight(.85f)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Text("Display",color=white,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
      Text(if(Settings.System.canWrite(ctx)) "BRIGHTNESS" else "ALLOW ACCESS",color=if(Settings.System.canWrite(ctx)) muted else Color(0xFFFFD18A),fontSize=9.sp)
     }
     Row(verticalAlignment=Alignment.CenterVertically) {
      Icon(Icons.Default.Brightness6,null,tint=white,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(7.dp))
      Slider(bright,{v-> if(Settings.System.canWrite(ctx)){bright=v;Settings.System.putInt(ctx.contentResolver,Settings.System.SCREEN_BRIGHTNESS,(v*255).toInt().coerceIn(1,255))}else if(!asked){asked=true;permission.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:${ctx.packageName}")))}},colors=SliderDefaults.colors(thumbColor=white,activeTrackColor=blue,inactiveTrackColor=white.copy(alpha=.18f)))
     }
     Row(verticalAlignment=Alignment.CenterVertically){Text("Dark appearance",color=muted,modifier=Modifier.weight(1f),fontSize=12.sp);Switch(dark,{dark=it},colors=SwitchDefaults.colors(checkedTrackColor=blue))}
    }
    Text("Liquid Glass • iOS-inspired design",color=white.copy(alpha=.45f),fontSize=10.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
   }
  }
 }
}
private fun readBrightness(c:Context)=(Settings.System.getInt(c.contentResolver,Settings.System.SCREEN_BRIGHTNESS,128)/255f).coerceIn(.01f,1f)
private fun open(c:Context,a:String){try{c.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:Exception){c.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
@Composable private fun CircleButton(icon:ImageVector,tint:Color,onClick:()->Unit){Surface(onClick=onClick,shape=CircleShape,color=Color.White.copy(alpha=.13f),border=BorderStroke(1.dp,Color.White.copy(alpha=.2f))){Icon(icon,null,tint=tint,modifier=Modifier.padding(12.dp).size(21.dp))}}
@Composable private fun Tile(label:String,icon:ImageVector,active:Boolean,accent:Color,ink:Color,modifier:Modifier=Modifier,onClick:()->Unit){Surface(onClick=onClick,modifier=modifier.fillMaxHeight(),shape=RoundedCornerShape(19.dp),color=if(active)accent.copy(alpha=.30f) else Color.White.copy(alpha=.10f),border=BorderStroke(1.dp,Color.White.copy(alpha=.18f))){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(icon,null,tint=ink,modifier=Modifier.size(22.dp));Spacer(Modifier.height(4.dp));Text(label,color=ink,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun GlassBox(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Surface(modifier=modifier.fillMaxWidth().shadow(13.dp,RoundedCornerShape(25.dp)),shape=RoundedCornerShape(25.dp),color=Color.White.copy(alpha=.105f),border=BorderStroke(1.dp,Color.White.copy(alpha=.21f))){Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp),content=content)}}
