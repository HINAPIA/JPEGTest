package com.example.camerax.PictureModule

import android.util.Log


class PictureInfo(_groupID : Int, _Picture : Picture) {
    var groupID : Int? = null
    var typeCode : Int? = null
    var offset : Int? = null
    var size : Int? = null
    init {
        groupID = _groupID
        typeCode = _Picture.getImageType().code
        size = _Picture.getByteArray().size
        Log.d("Picture Module", "[PictureInfor 생성] : " +
                "groupID ${groupID}, typeCode : ${typeCode}, size : ${size}")
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