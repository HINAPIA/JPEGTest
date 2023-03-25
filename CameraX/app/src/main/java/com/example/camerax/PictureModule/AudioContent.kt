package com.example.camerax.PictureModule

import com.example.camerax.PictureModule.Contents.Attribute
import com.example.camerax.PictureModule.Contents.Audio

class AudioContent {
    lateinit var audio : Audio
    var length = 0
    init {

    }

    fun init() {
        length = 0
        TODO("Not yet implemented")
    }
    suspend fun reFresh(byteArrayList: ArrayList<ByteArray>, attribute : Attribute){
        for(i in 0..byteArrayList.size-1){
            // audio 객체 생성
            var audio = Audio(byteArrayList.get(i),attribute)
            this.audio = audio
        }
        length = audio.audioByteArray.size
    }

}