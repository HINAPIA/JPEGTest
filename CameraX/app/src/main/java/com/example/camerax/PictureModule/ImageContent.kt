package com.example.camerax.PictureModule

import android.util.Log
import com.example.camerax.PictureModule.Contents.Attribute
import java.nio.Buffer
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

    fun reFresh(byteArrayList: ArrayList<ByteArray>, attribute : Attribute){
        for(i in 0..byteArrayList.size-1){
            // Picture 객체 생성
            var picture = Picture(byteArrayList.get(i), attribute)
            insertPicture(picture)
        }
    }

    fun getHeaderInfo(): ByteArray {
        Log.d("header Info", "getHeaderInfo===============================================")

        var offsetList : ArrayList<Int> = arrayListOf()
        offsetList.add(0)
        var offset = 0
        var imgaeContentInfoSize = 0
        // imgaeContentInfoSize 구하기
        for(i in 0..pictureList.size -1){
            var picture = pictureList.get(i)
            imgaeContentInfoSize += picture.getInfoLength()
        }
        // imageContentInfoSize, ImgaeStartOffset, ImageCount. 4X3
        imgaeContentInfoSize += 12
        val buffer: ByteBuffer = ByteBuffer.allocate(imgaeContentInfoSize)
        buffer.putInt(imgaeContentInfoSize)
        Log.d("header Info", "ImageContentInfo size : ${imgaeContentInfoSize} (4)")

        buffer.putInt(0)
        Log.d("header Info", "ImageContent Start offset : 0(4)")

        var ImageCount = pictureCount
        buffer.putInt(ImageCount)
        Log.d("header Info", "ImageCount : ${pictureCount}(4)")

            // offset, attribue, size, 추가데이터 크기, 추가데이터
        var preSize = 0
        for(i in 0..pictureList.size -1){
            var picture = pictureList.get(i)
            var size = picture.pictureByteArray.size
            var attribute = picture.attribute.code
            var embeddedSize = picture.embeddedSize
            var embeddedData = picture.embeddedData
            if(i > 0){
                offset = offset + preSize
            }
            preSize = size
            Log.d("header Info", "============= picture#${i+1} info start================")
            buffer.putInt(offset)
            buffer.putInt(size)
            buffer.putInt(attribute)
            buffer.putInt(embeddedSize)
            Log.d("header Info", "offset : ${offset}(4)")
            Log.d("header Info", "size : ${size}(4)")
            Log.d("header Info", "attribute : ${attribute}(4)")
            Log.d("header Info", "embeddedSize : ${embeddedSize}(4)")
            if(picture.embeddedSize > 0){
                buffer.put(embeddedData)
                Log.d("header Info", "embeddedData : ${embeddedData}(${embeddedSize})")
            }
        }
        Log.d("header Info", "end =================================")
        return buffer.array()
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

    //PictureList의 모든 Picture의 Info 사이즈를 리턴
    fun getInfoLength() : Int{
        var sum = 0
        for(i in 0..pictureList.size -1){
            sum += pictureList.get(i).getInfoLength()
        }
        return sum
    }
}