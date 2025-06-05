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
import android.widget.RemoteViews
import android.widget.TextView
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
                    showNotification("Music", path.substringAfterLast("/"))
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @SuppressLint("ForegroundServiceType")
    internal fun showNotification(title: String, content: String) {
        val manager : NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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

        val remoteView = RemoteViews(packageName, R.layout.custom_notificaiton)

        remoteView.setTextViewText(R.id.notification_title, content)
        remoteView.setImageViewResource(
            R.id.notification_prev,
            R.drawable.previous_back_direction_svgrepo_com
        )
        remoteView.setImageViewResource(
            R.id.notification_play_pause,
            if (mediaPlayer?.isPlaying == true) {
                R.drawable.pause_svgrepo_com
            } else {
                R.drawable.play_svgrepo_com
            }
        )
        remoteView.setImageViewResource(R.id.notification_next, R.drawable.next_svgrepo_com)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) FLAG_IMMUTABLE else 0


        remoteView.setOnClickPendingIntent(
            R.id.notification_prev,
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_PREV), flag)
        )


        val playPauseIntent = PendingIntent.getBroadcast(this, 1, Intent(ACTION_PLAY_PAUSE), flag)

        if (mediaPlayer?.isPlaying == true) {
            remoteView.setImageViewResource(
                R.id.notification_play_pause,
                R.drawable.pause_svgrepo_com
            )
        } else {
            remoteView.setImageViewResource(
                R.id.notification_play_pause,
                R.drawable.play_svgrepo_com
            )
        }

        remoteView.setOnClickPendingIntent(R.id.notification_play_pause, playPauseIntent)




        remoteView.setOnClickPendingIntent(
            R.id.notification_next,
            PendingIntent.getBroadcast(this, 2, Intent(ACTION_NEXT), flag)
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteView)
            .setCustomBigContentView(remoteView)

            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()


        manager.notify(1, notification)

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
