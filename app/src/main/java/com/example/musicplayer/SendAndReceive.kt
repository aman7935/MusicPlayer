package com.example.musicplayer

interface SendAndReceive {
    fun nextSong(position : Int)
    fun pauseSong()
    fun prevSong(position: Int)

}