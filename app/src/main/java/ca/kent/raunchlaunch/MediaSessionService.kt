package ca.kent.raunchlaunch

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*

object MediaStateHolder {
    var mediaInfo by mutableStateOf<MediaInfo?>(null)
    var bluetoothInfo by mutableStateOf<BluetoothInfo?>(null)
    var currentVolume by mutableStateOf(0f)
}

class MediaSessionService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo()
            syncProgress()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo()
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        val newController = controllers?.firstOrNull()
        if (newController != activeController) {
            activeController?.unregisterCallback(callback)
            activeController = newController
            activeController?.registerCallback(callback)
            updateMediaInfo()
            syncProgress()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBluetoothInfo()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaSessionService::class.java)
        
        mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, componentName)
        
        val controllers = try { mediaSessionManager?.getActiveSessions(componentName) } catch (e: Exception) { null }
        activeController = controllers?.firstOrNull()
        activeController?.registerCallback(callback)
        
        updateMediaInfo()
        syncProgress()
        updateBluetoothInfo()
        updateVolumeInfo()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, filter)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        activeController?.unregisterCallback(callback)
        activeController = null
        MediaStateHolder.mediaInfo = null
        progressJob?.cancel()
        try { unregisterReceiver(bluetoothReceiver) } catch (e: Exception) {}
    }

    private fun syncProgress() {
        progressJob?.cancel()
        if (activeController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
            progressJob = serviceScope.launch {
                while (isActive) {
                    updateMediaInfo()
                    delay(1000)
                }
            }
        }
    }

    private fun updateMediaInfo() {
        val controller = activeController
        if (controller == null) {
            MediaStateHolder.mediaInfo = null
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        MediaStateHolder.mediaInfo = MediaInfo(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
            position = playbackState?.position ?: 0L,
            duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            controller = controller
        )
        updateVolumeInfo()
    }

    @SuppressLint("MissingPermission")
    private fun updateBluetoothInfo() {
        try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bm.adapter
            if (adapter != null && adapter.isEnabled) {
                val device = adapter.bondedDevices.firstOrNull() 
                if (device != null) {
                    MediaStateHolder.bluetoothInfo = BluetoothInfo(
                        deviceName = device.name ?: "Unknown Device",
                        batteryLevel = -1
                    )
                } else {
                    MediaStateHolder.bluetoothInfo = null
                }
            }
        } catch (e: SecurityException) {
            MediaStateHolder.bluetoothInfo = null
        }
    }

    private fun updateVolumeInfo() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        MediaStateHolder.currentVolume = current.toFloat() / max
    }
}

data class MediaInfo(
    val title: String,
    val artist: String,
    val artwork: android.graphics.Bitmap?,
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long,
    val controller: MediaController? = null
)

data class BluetoothInfo(
    val deviceName: String,
    val batteryLevel: Int // -1 if unknown
)
