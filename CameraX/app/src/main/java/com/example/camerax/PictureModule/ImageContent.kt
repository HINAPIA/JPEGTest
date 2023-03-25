package com.example.camerax.PictureModule

import com.example.camerax.PictureModule.Contents.Attribute

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

    fun reFresh(byteArrayList: ArrayList<ByteArray>, attribute : Attribute){
        for(i in 0..byteArrayList.size-1){
            // Picture 객체 생성
            var picture = Picture(byteArrayList.get(i), attribute)
            insertPicture(picture)
        }
    }

    fun insertPicture(picture : Picture){
        pictureList.add(picture)
        pictureCount = pictureCount + 1
    }

}