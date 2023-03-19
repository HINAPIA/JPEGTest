package com.example.camerax.PictureModule

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log

enum class ImageType(val code : Int ) {
    basic(0),
    focus(1),
    modified(2),
    edited(3)
}

class Picture(_byteArray: ByteArray, _imageType : ImageType) {
    private var bitmap : Bitmap
    private var byteArray : ByteArray
    private var imageType : ImageType
    private var imageSize : Int
    init {
        byteArray = _byteArray
        imageType = _imageType
        imageSize = byteArray.size
        bitmap = byteArrayToBitmap(byteArray)
        Log.d("Picture Module",
            "[create Picture]size :${byteArray.size}, type: ${imageType}")
    }

    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun getImageType() : ImageType{
        return imageType
    }
    fun getByteArray() : ByteArray{
        return byteArray
    }
}