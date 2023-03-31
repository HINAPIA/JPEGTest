package com.example.camerax.PictureModule

import android.util.Log
import com.example.camerax.PictureModule.Contents.ContentAttribute
import java.nio.ByteBuffer

// 하나 이상의 Picture(이미지)를 담는 컨테이너
class ImageContent {

    var pictureList : ArrayList<Picture> = arrayListOf()
    var pictureCount = 0

    lateinit var modifiedPicture : Picture
    fun init() {
        pictureList.clear()
        pictureCount = 0
    }
    // ImageContent 리셋 후 초기화 - 카메라 찍을 때 호출되는 함수
    fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        init()
        for(i in 0..byteArrayList.size-1){
            // 헤더 분리
            // frame 분리
            // Picture 객체 생성
            var picture = Picture(byteArrayList.get(i), contentAttribute)
            var intList : ArrayList<Int> = arrayListOf(1,2,3,4,5)
            picture.insertEmbeddedData(intList)
            insertPicture(picture)
        }
    }
    // ImageContent 리셋 후 초기화 - 파일을 parsing할 때 ImageContent를 생성
    fun setContent(_pictureList : ArrayList<Picture>){
        init()
        pictureList = _pictureList
        pictureCount = _pictureList.size
    }
    fun addContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
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