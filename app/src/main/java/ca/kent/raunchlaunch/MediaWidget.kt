package ca.kent.raunchlaunch

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MediaWidget(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val mediaInfo = MediaStateHolder.mediaInfo
    val bluetoothInfo = MediaStateHolder.bluetoothInfo
    val volume = MediaStateHolder.currentVolume

    val widgetWidth = if (isLandscape) 180.dp else 340.dp
    val widgetHeight = if (isLandscape) 340.dp else 140.dp

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        modifier = modifier.padding(top = if (isLandscape) 16.dp else 60.dp, start = 16.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(widgetWidth)
                .height(widgetHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161B22).copy(alpha = 0.9f))
                .border(1.dp, Color(0xFF58A6FF).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (isLandscape) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MediaArt(mediaInfo, Modifier.size(100.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    MediaText(mediaInfo, Alignment.CenterHorizontally)
                    Spacer(modifier = Modifier.weight(1f))
                    PlaybackProgress(mediaInfo)
                    MediaControls(mediaInfo)
                    Spacer(modifier = Modifier.height(8.dp))
                    SystemStatus(bluetoothInfo, volume)
                }
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MediaArt(mediaInfo, Modifier.size(64.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            MediaText(mediaInfo, Alignment.Start)
                            Spacer(modifier = Modifier.height(4.dp))
                            SystemStatus(bluetoothInfo, volume)
                        }
                        MediaControls(mediaInfo)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PlaybackProgress(mediaInfo)
                }
            }
        }
    }
}

@Composable
fun PlaybackProgress(info: MediaInfo?) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Update slider only when not dragging and song is playing
    LaunchedEffect(info?.position, isDragging) {
        if (!isDragging && info != null && info.duration > 0) {
            sliderValue = info.position.toFloat() / info.duration
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Slider(
            value = sliderValue,
            onValueChange = {
                isDragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                info?.let {
                    val newPos = (sliderValue * it.duration).toLong()
                    it.controller?.transportControls?.seekTo(newPos)
                }
            },
            modifier = Modifier.height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF),
                activeTrackColor = Color(0xFF58A6FF),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val displayPos = if (isDragging && info != null) (sliderValue * info.duration).toLong() else (info?.position ?: 0)
            Text(formatTime(displayPos), color = Color.Gray, fontSize = 9.sp)
            Text(formatTime(info?.duration ?: 0), color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun SystemStatus(bluetooth: BluetoothInfo?, volume: Float) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Slider(
                value = volume,
                onValueChange = {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val newVolume = (it * max).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                    MediaStateHolder.currentVolume = it
                },
                modifier = Modifier.width(80.dp).height(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White.copy(alpha = 0.7f),
                    activeTrackColor = Color.White.copy(alpha = 0.5f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
            Text("${(volume * 100).toInt()}%", color = Color.Gray, fontSize = 9.sp)
        }
        
        if (bluetooth != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Icon(Icons.Default.Bluetooth, null, tint = Color(0xFF58A6FF), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = bluetooth.deviceName.uppercase(),
                    color = Color(0xFF58A6FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MediaArt(info: MediaInfo?, modifier: Modifier) {
    val scale by animateFloatAsState(if (info?.isPlaying == true) 1f else 0.92f)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
    ) {
        if (info?.artwork != null) {
            Image(
                bitmap = info.artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("♪", color = Color(0xFF58A6FF).copy(alpha = 0.3f), fontSize = 32.sp)
            }
        }
    }
}

@Composable
fun MediaText(info: MediaInfo?, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = (info?.title ?: "NOT PLAYING").uppercase(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            letterSpacing = 1.sp,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = (info?.artist ?: "READY").uppercase(),
            color = Color(0xFF8B949E),
            fontSize = 10.sp,
            maxLines = 1,
            letterSpacing = 1.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MediaControls(info: MediaInfo?) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val playScale by animateFloatAsState(if (isPressed) 0.8f else 1f, label = "playScale")

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { info?.controller?.transportControls?.skipToPrevious() }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        IconButton(
            onClick = { 
                if (info?.isPlaying == true) info.controller?.transportControls?.pause()
                else info?.controller?.transportControls?.play()
            },
            interactionSource = interactionSource,
            modifier = Modifier.scale(playScale)
        ) {
            Icon(
                if (info?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint = Color(0xFF58A6FF),
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = { info?.controller?.transportControls?.skipToNext() }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return "%d:%02d".format(min, sec)
}
