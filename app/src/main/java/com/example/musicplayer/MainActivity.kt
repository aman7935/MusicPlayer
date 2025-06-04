package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.example.musicplayer.Utils.ACTION_NEXT
import com.example.musicplayer.Utils.ACTION_PLAY_PAUSE
import com.example.musicplayer.Utils.ACTION_PREV
import com.example.musicplayer.databinding.ActivityMainBinding
import com.example.musicplayer.datamodel.MusicInfo


class MainActivity : AppCompatActivity(), SendAndReceive {
    private lateinit var binding: ActivityMainBinding
    private var musicService: MusicService? = null
    var isBound = false
    val musicList = mutableListOf<MusicInfo>()
    internal var position = 0
    private val isPlaying = false

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
                        Toast.makeText(
                            this@MainActivity,
                            "Previous button is clicked",
                            Toast.LENGTH_LONG
                        ).show()
                        position--
                        binding.name.text = musicList[position].title
                        musicService?.playMusic(musicList[position].data)
                    }
                }

                ACTION_PLAY_PAUSE -> pauseBtn()

                ACTION_NEXT -> nextBtn()
                else -> "Unknown action"
            }
        }
    }

    private fun pauseBtn() {
        Toast.makeText(this@MainActivity, "Play pause button is clicked", Toast.LENGTH_LONG)
            .show()
        if (musicService?.mediaPlayer?.isPlaying == true) {
            musicService?.mediaPlayer?.pause()

            binding.playPauseBtn.setImageResource(R.drawable.play_svgrepo_com)
        } else {
            musicService?.mediaPlayer?.start()
            binding.playPauseBtn.setImageResource(R.drawable.pause_svgrepo_com)
        }
    }

    private fun nextBtn() {
        Toast.makeText(this@MainActivity, "Next button is clicked", Toast.LENGTH_LONG).show()
        if (position != musicList.lastIndex) {
            position++
            binding.name.text = musicList[position].title
            musicService?.playMusic(musicList[position].data)
            binding.playPauseBtn.setImageResource(R.drawable.pause_svgrepo_com)
            Toast.makeText(this@MainActivity, "$position", Toast.LENGTH_LONG).show()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ACTION_PREV)
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
        }
        registerReceiver(notificationReceiver, filter, RECEIVER_EXPORTED)
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
        binding.bottomSheet.visibility = View.GONE
        setUpButtons()
        binding.name.isSelected = true



        binding.name.setOnClickListener {
            val sheet = BottomSheet(position, musicList)

            sheet.show(supportFragmentManager, "BottomSheet")
        }


        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    musicService?.mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun setUpButtons() {
        binding.apply {

            playPauseBtn.setOnClickListener {
                pauseBtn()
            }
            nextBtn.setOnClickListener {
                nextBtn()
            }


        }
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
                    binding.seekBar.max = data.duration.toInt()
                    TransitionManager.beginDelayedTransition(binding.main, AutoTransition())
                    binding.bottomSheet.visibility = View.VISIBLE
                    binding.playPauseBtn.setImageResource(R.drawable.pause_svgrepo_com)


                    binding.name.text = data.title
                    musicService?.playMusic(data.data)
                    position = p;

                    initSeekBar()

                }
            })
        }
    }

    fun initSeekBar() {
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(object : Runnable {
            @SuppressLint("DefaultLocale")
            override fun run() {
                try {
                    binding.seekBar.progress = musicService?.mediaPlayer!!.currentPosition
                    binding.text2.text = String.format(
                        "%02d:%02d",
                        (musicService?.mediaPlayer!!.currentPosition / 1000 / 60),
                        (musicService?.mediaPlayer!!.currentPosition / 1000 % 60)
                    )
                    binding.totalDuration.text = String.format(
                        "%02d:%02d",
                        (musicService?.mediaPlayer!!.duration / 1000 / 60),
                        (musicService?.mediaPlayer!!.duration / 1000 % 60)
                    )
                    binding.seekBar.max = musicService?.mediaPlayer!!.duration
                    Log.d(TAG, "run:${musicService?.mediaPlayer!!.duration}")

                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.seekBar.progress = 0
                }
            }
        }, 0)
    }

    override fun nextSong(position: Int) {
        Log.d(TAG, "sendData $position")
        if (position != musicList.lastIndex){
            binding.name.text = musicList[position].title
            musicService?.playMusic(musicList[position].data)
        }
    }

    override fun pauseSong() {
        if (position != 0){

            musicService?.mediaPlayer?.stop()
        }
    }

    override fun prevSong(position: Int) {
        if (position != 0){
            musicService?.playMusic(musicList[position].data)

        }
    }


}