package com.example.musicplayer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.musicplayer.Utils.ACTION_NEXT
import com.example.musicplayer.Utils.ACTION_PLAY_PAUSE
import com.example.musicplayer.Utils.ACTION_PREV

class MusicService : Service() {

    internal var mediaPlayer: MediaPlayer? = null
    private val binder = MusicBinder()



    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification("Music Player", "Playing Music")

        return START_STICKY
    }

    fun playMusic(path: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(path)
                setOnPreparedListener {
                    it.start()
                    showNotification("Music Player", path.substringAfterLast("/"))
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @SuppressLint("ForegroundServiceType")
    private fun showNotification(title: String, content: String) {
        val channelId = "music_channel"
        val channelName = "Music Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) FLAG_IMMUTABLE else 0

        val pendingIntentPrev = PendingIntent.getBroadcast(this, 0, Intent(ACTION_PREV), flag)
        val pendingIntentNext = PendingIntent.getBroadcast(this, 1, Intent(ACTION_NEXT), flag)
        val pendingIntentPlayPause = PendingIntent.getBroadcast(this, 2, Intent(ACTION_PLAY_PAUSE), flag)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .addAction(0, "Previous", pendingIntentPrev)
            .addAction(0, "pause", pendingIntentPlayPause)
            .addAction(0, "Next", pendingIntentNext)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaPlayer?.release()
        mediaPlayer?.stop()
        mediaPlayer = null
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
