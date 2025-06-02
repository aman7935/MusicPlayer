package com.example.musicplayer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.databinding.MusicItemViewBinding
import com.example.musicplayer.datamodel.MusicInfo

class MusicAdapter(private val musicList : List<MusicInfo>, private val listener : OnItemClickListener) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    inner class MusicViewHolder(val binding : MusicItemViewBinding) : RecyclerView.ViewHolder(binding.root){

        fun bindData(musicInfo: MusicInfo){
            binding.apply {
                title.text = musicInfo.title
                artist.text = musicInfo.artist
            }
            itemView.setOnClickListener {
                listener.onClick(musicList[position], position)
            }
        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MusicAdapter.MusicViewHolder {
        val binding = MusicItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicAdapter.MusicViewHolder, position: Int) {
        holder.bindData(musicList[position])
    }

    override fun getItemCount(): Int = musicList.size
}