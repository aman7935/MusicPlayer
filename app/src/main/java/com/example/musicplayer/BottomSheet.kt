package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import com.example.musicplayer.databinding.MusicBottomSheetBinding
import com.example.musicplayer.datamodel.MusicInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheet(private var position: Int, private val musicList: MutableList<MusicInfo>) : BottomSheetDialogFragment() {

    private lateinit var binding: MusicBottomSheetBinding
    private var sendAndReceive: SendAndReceive? = null
    private var musicService: MusicService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val localBinder = service as MusicService.MusicBinder
            musicService = localBinder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SendAndReceive) sendAndReceive = context
        else throw RuntimeException("$context must implement SendAndReceive")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MusicBottomSheetBinding.inflate(layoutInflater)
        binding.name.isSelected = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name.text = musicList[position].data.substringAfterLast("/")
        binding.totalDuration.text = formatTime(musicList[position].duration)

        binding.downBtn.setOnClickListener { dismiss() }
        initSeekBar()

        binding.nextBtn.setOnClickListener {
            if (position != musicList.lastIndex) {
                position++
                sendAndReceive?.nextSong(position)
                binding.name.text = musicList[position].title
                Toast.makeText(requireContext(), "$position", Toast.LENGTH_LONG).show()
            }
        }

        binding.playPauseBtn.setOnClickListener {
            musicService?.mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    binding.playPauseBtn.setImageResource(R.drawable.play_button_svgrepo_com)
                } else {
                    player.start()
                    binding.playPauseBtn.setImageResource(R.drawable.pause_svgrepo_com)
                }
            }
        }

        binding.previousBtn.setOnClickListener {
            if (position != 0) {
                position--
                binding.name.text = musicList[position].title
                sendAndReceive?.prevSong(position)
            }
        }
    }

    private fun initSeekBar() {
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(object  : Runnable{
            override fun run() {
                musicService?.mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPosition = player.currentPosition
                        binding.seekBar.progress = currentPosition
                        binding.text2.text = formatTime(currentPosition.toLong())
                        binding.seekBar.max = player.duration
                    }
                }
            }
        }, 0)
    }

    private fun formatTime(ms: Long): String {
        val minutes = ms / 1000 / 60
        val seconds = ms / 1000 % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        Intent(requireContext(), MusicService::class.java).also {
            requireContext().bindService(it, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
    }
}
