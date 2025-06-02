package com.example.musicplayer

import com.example.musicplayer.datamodel.MusicInfo

interface OnItemClickListener {

    fun onClick(data: MusicInfo, position: Int)
}