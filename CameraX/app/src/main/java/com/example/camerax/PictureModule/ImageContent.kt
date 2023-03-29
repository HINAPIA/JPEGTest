package com.example.camerax.PictureModule

import android.util.Log
import com.example.camerax.PictureModule.Contents.ContentAttribute
import java.nio.ByteBuffer

// 하나 이상의 Picture(이미지)를 담는 컨테이너
class ImageContent {

    var pictureList : ArrayList<Picture> = arrayListOf()
    var length = 0
    var pictureCount = 0

    fun init() {
        pictureList.clear()
        length = 0
        pictureCount = 0
    }

    fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        for(i in 0..byteArrayList.size-1){
            // Picture 객체 생성
            var picture = Picture(byteArrayList.get(i), contentAttribute)
            insertPicture(picture)
        }
    }


    // PictureList에 Picture를 삽입
    fun insertPicture(picture : Picture){
        pictureList.add(picture)
        pictureCount = pictureCount + 1
    }

    // PictureList의 index번째 요소를 찾아 반환
    fun getPictureAtIndex(index : Int): Picture? {
        return pictureList.get(index) ?: null
    }


}