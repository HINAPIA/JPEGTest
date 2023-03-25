package com.example.camerax.PictureModule.Contents

import android.graphics.Bitmap
import android.util.Log
import com.example.camerax.PictureModule.Picture


class Text (_byteArray : ByteArray, _attribute: Attribute) {

    var textByteArray : ByteArray
    var attribute : Attribute
    private var length : Int = 0
    init {
        textByteArray = _byteArray
        attribute = _attribute
        Log.d("Picture Module",
            "[create Text]size :${textByteArray.size}")
    }

    fun getInfoLength() : Int{
        // offset(4) + attribute(4) + size(4)
        return 12
    }
}