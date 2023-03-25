package com.example.camerax.PictureModule.Contents

import android.util.Log

class Audio (_audioByteArray : ByteArray, _attribute: Attribute){
    var audioByteArray : ByteArray
    var attribute = _attribute
    private var length : Int = 0
    init {
        audioByteArray = _audioByteArray
        attribute = _attribute
        Log.d("Picture Module",
            "[create Audio]size :${audioByteArray.size}")
    }
}