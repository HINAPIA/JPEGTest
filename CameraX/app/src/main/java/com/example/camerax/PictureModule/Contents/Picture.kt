package com.example.camerax.PictureModule

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.camerax.PictureModule.Contents.Attribute



class Picture(_byteArray: ByteArray, _attribute: Attribute) {
    private  var bitmap : Bitmap
    var pictureByteArray : ByteArray
    var attribute : Attribute
    var embeddedSize = 0
    var embeddedData : ByteArray? = null
    init {
        pictureByteArray = _byteArray
        bitmap = byteArrayToBitmap(pictureByteArray)
        attribute = _attribute
        Log.d("Picture Module",
            "[create Picture]size :${pictureByteArray.size}, attribute: ${attribute}")
    }

    //추가 데이터를 셋팅하는 함수
    fun insertEmbeddedData(data : ByteArray){
        this.embeddedData = data
        this.embeddedSize = data.size
    }
    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun getInfoLength() : Int{
        // offset(4) + attribute(4) + size(4) + embedded size(4) + embedded Data(var)
        return 16 + embeddedSize
    }

}