package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.Utils.ACTION_NEXT
import com.example.musicplayer.Utils.ACTION_PLAY_PAUSE
import com.example.musicplayer.Utils.ACTION_PREV
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.datamodel.MusicInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var musicService: MusicService? = null
    var isBound = false
    val musicList = mutableListOf<MusicInfo>()
    private var position = 0
    private var firstIndex = 0
    private val lastIndex = 0

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val musicBinder = service as MusicService.MusicBinder
            musicService = musicBinder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }


    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PREV -> {

                    if (position != 0) {
                        Toast.makeText(context, "Previous button is clicked", Toast.LENGTH_LONG)
                            .show()
                        position--
                        musicService?.playMusic(musicList[position].data)
                    }
                }

                ACTION_PLAY_PAUSE -> {
                    Toast.makeText(context, "Play pause button is clicked", Toast.LENGTH_LONG)
                        .show()
                }

                ACTION_NEXT -> {
                    Toast.makeText(context, "Next button is clicked", Toast.LENGTH_LONG).show()
                    if (position != musicList.lastIndex) {
                        position++
                        musicService?.playMusic(musicList[position].data)
                        Toast.makeText(this@MainActivity, "$position", Toast.LENGTH_LONG).show()
                    }
                }

                else -> "Unknown action"
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ACTION_PREV)
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
        }
        registerReceiver(notificationReceiver, filter)
        Intent(this, MusicService::class.java).also {
            ContextCompat.startForegroundService(this, it)
            bindService(it, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(notificationReceiver)
        if (isBound) {
            unbindService(connection)
            isBound = false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission()

       /* val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN*/

        binding.bottomSheet.visibility = View.GONE


    }

    private fun checkPermission() { //it
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        } else fetchMusic()
    }


    override fun onRequestPermissionsResult(
        /*what to do after the permission is granted or
        denied it only checks for the first time if the permission is given or not*/
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchMusic()

        }
    }

    private fun fetchMusic() {

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} !=0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} DESC"

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )
        cursor?.use {
            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationIndex = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val music = MusicInfo(
                    title = it.getString(titleIndex),
                    artist = it.getString(artistIndex),
                    data = it.getString(dataIndex),
                    duration = it.getLong(durationIndex)
                )

                musicList.add(music)
            }

            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = MusicAdapter(musicList, object : OnItemClickListener {
                override fun onClick(data: MusicInfo, p: Int) {
                    /*val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED*/
                    binding.seekBar.max = data.duration.toInt()
                    binding.bottomSheet.visibility  = View.VISIBLE

                    binding.name.text = data.title

                    musicService?.playMusic(data.data)
                    position = p;
                    Toast.makeText(this@MainActivity, "$p", Toast.LENGTH_LONG).show()
                }
            })
        }
    }


}