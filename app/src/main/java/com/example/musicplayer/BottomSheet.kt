package com.example.musicplayer

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
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

class BottomSheet(
    private var position: Int,
    private val musicList: MutableList<MusicInfo>
) : BottomSheetDialogFragment() {

    private lateinit var binding: MusicBottomSheetBinding
    private var sendAndReceive: SendAndReceive? = null
    private var musicService: MusicService? = null
    private var isBound = false

    private lateinit var handler: Handler
    private lateinit var updateSeekBarRunnable: Runnable

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
    ): View {
        binding = MusicBottomSheetBinding.inflate(layoutInflater)
        binding.name.isSelected = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.name.text = musicList[position].data.substringAfterLast("/")
        binding.totalDuration.text = formatTime(musicList[position].duration)

        binding.downBtn.setOnClickListener { dismiss() }

        binding.nextBtn.setOnClickListener {
            if (position != musicList.lastIndex) {
                position++
                sendAndReceive?.nextSong(position)
                binding.name.text = musicList[position].title
                binding.totalDuration.text = formatTime(musicList[position].duration)
            }
        }

        binding.playPauseBtn.setOnClickListener {
            musicService?.mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    binding.playPauseBtn.setImageResource(R.drawable.play_svgrepo_com)
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
                binding.totalDuration.text = formatTime(musicList[position].duration)
                sendAndReceive?.prevSong(position)
            }
        }

        initSeekBar()
    }

    private fun initSeekBar() {
        handler = Handler(Looper.getMainLooper())
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                musicService?.mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPosition = player.currentPosition
                        binding.seekBar.progress = currentPosition
                        binding.text2.text = formatTime(currentPosition.toLong())
                        binding.seekBar.max = player.duration
                    }
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(updateSeekBarRunnable)

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
            behavior.addBottomSheetCallback(object  : BottomSheetBehavior.BottomSheetCallback(){
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED || behavior.state == BottomSheetBehavior.STATE_HIDDEN){
                        dismiss()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset <0.96){
                        binding.downBtn.visibility = View.INVISIBLE
                        binding.lineBtn.visibility = View.VISIBLE
                    }
                    else{
                        binding.downBtn.visibility = View.VISIBLE
                        binding.lineBtn.visibility = View.INVISIBLE
                    }
                }

            })
        }

        Intent(requireContext(), MusicService::class.java).also {
            requireContext().bindService(it, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::handler.isInitialized && ::updateSeekBarRunnable.isInitialized) {
            handler.removeCallbacks(updateSeekBarRunnable)
        }

        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
    }
}
