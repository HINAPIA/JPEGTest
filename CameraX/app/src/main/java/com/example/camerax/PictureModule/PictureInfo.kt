package com.example.camerax.PictureModule

import android.util.Log
import java.io.Serializable


class PictureInfo() {
    var groupID : Int? = null
    var typeCode : Int? = null
    var offset : Int? = null
    var size : Int? = null

    constructor(_groupID : Int, _Picture : Picture) : this() {
        groupID = _groupID
        typeCode = _Picture.getImageType().code
        size = _Picture.getByteArray().size
        Log.d("Picture Module", "[PictureInfor 생성] : " +
                "groupID ${groupID}, typeCode : ${typeCode}, size : ${size}")

    }
    constructor(_groupID : Int,_typeCode : Int,_offset : Int,_size : Int) : this() {
        groupID = _groupID
        typeCode = _typeCode
        size = _size
        offset = _offset
    }
    fun setOffset(_offset : Int){
        offset = _offset
        Log.d("Picture Module", "[PictureInfor 생성] : " +
                "offset ${offset}")
    }
//    fun getSize() : Int? {
//        return size
//    }

}