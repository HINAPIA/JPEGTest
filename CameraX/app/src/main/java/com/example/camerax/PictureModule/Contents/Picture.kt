package com.example.camerax.PictureModule

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.camerax.PictureModule.Contents.Attribute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Picture(_byteArray: ByteArray, _attribute: Attribute) {
    private lateinit var bitmap : Bitmap
    var pictureByteArray : ByteArray
    var size : Int
    var attribute : Attribute
    var embeddedSize = 0
    var embeddedData : ByteArray? = null
    var offset = 0
    init {

            pictureByteArray = _byteArray
            size = pictureByteArray.size
            attribute = _attribute
        CoroutineScope(Dispatchers.IO).launch {
            bitmap = byteArrayToBitmap(_byteArray)

            Log.d("Picture Module",
                "[create Picture]size :${pictureByteArray.size}, attribute: ${attribute}")
        }

    }
    constructor(
        _offset: Int,
        _byteArray: ByteArray, _attribute: Attribute, _embeddedSize: Int, _embeddedData: ByteArray?
    ) : this(_byteArray,_attribute){
        offset = _offset
        embeddedSize = _embeddedSize
        embeddedData = _embeddedData
    }

    //추가 데이터를 셋팅하는 함수
    fun insertEmbeddedData(data : ByteArray){
        this.embeddedData = data
        this.embeddedSize = data.size
    }
    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(_byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(_byteArray, 0, _byteArray.size)
    }

    fun getInfoLength() : Int{
        // offset(4) + attribute(4) + size(4) + embedded size(4) + embedded Data(var)
        return 16 + embeddedSize
    }

    fun getBitmap():Bitmap{
        return bitmap
    }

}