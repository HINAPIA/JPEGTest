package com.example.camerax.LoadModule

import android.app.Activity
import com.example.camerax.PictureModule.*
import com.example.camerax.PictureModule.Contents.Attribute
import com.google.firebase.components.ComponentRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoadResolver(_activity: Activity) {

    fun ByteArraytoInt(byteArray: ByteArray, stratOffset : Int): Int {
        var intNum :Int = ((byteArray[stratOffset].toInt() and 0xFF) shl 24) or
                ((byteArray[stratOffset+1].toInt() and 0xFF) shl 16) or
                ((byteArray[stratOffset+2].toInt() and 0xFF) shl 8) or
                ((byteArray[stratOffset+3].toInt() and 0xFF))
        return intNum
    }
    fun createMCContainer(activity: Activity, MCContainer: MCContainer, sourceByteArray: ByteArray){
        // var header : Header = Header()

        // start header parsing ...
        var imageContent : ImageContent= ImageContent()
        val intList = mutableListOf<Int>()
        //몇 개의 사진이 들어있는지 알아내기
        // APP3의 마커의 인덱스 위치를 2로 고정 시켜서 데이터의 시작은 4 인덱스 부터로 확신하여 짠 코드
        // 후에 App3 마커를 찾은 후부터 데이터를 parisng하도록 고쳐야함!

        // Imgae Content Parsing
        var imageInfoSize :Int = ByteArraytoInt(sourceByteArray,4)
        var ImageStartOffset : Int = ByteArraytoInt(sourceByteArray,8)
        var ImageCount : Int = ByteArraytoInt(sourceByteArray,12)
        //기록된 헤더 데이터를 4 바이트씩 끊어서 intList에 저장
        for (i in 16 .. 4 + imageInfoSize-1 step 4) {
            // ByteArray의 4바이트를 읽어서 Int로 변환하여 List에 추가
            intList.add(ByteArraytoInt(sourceByteArray,i))
        }

        CoroutineScope(Dispatchers.IO).launch {
            // Picture 생성
            for(i  in 0..intList.size-1 step 4){
                // 추가한 APP3 데이터 크기만큼 offset 증가
                var offset = if(i == 0)intList.get(i) else intList.get(i)+ imageInfoSize +1
                var size = if(i == 0) intList.get(i+1) + imageInfoSize else intList.get(i+1)
                var attribute =  intList.get(i+2)
                var embeddedSize = intList.get(i+3)
                if(embeddedSize > 0){
                    var length = embeddedSize/4

                }
                var startIndex = offset
                var endIndex = startIndex + size -1
                var length = endIndex - startIndex + 1
                val targetImageArray = ByteArray(length)
                // sliceArray() 함수를 사용하여 잘라낸 ByteArray를 생성
                sourceByteArray.sliceArray(startIndex..endIndex).copyInto(targetImageArray)
                var picture = Picture(offset, targetImageArray, Attribute.focus, embeddedSize, null)
                imageContent.insertPicture(picture)

            }
            MCContainer.imageContent = imageContent
            MCContainer.test = 99
        }
    }

    private var activity : Activity
    init{
        activity = _activity
    }
    //Jpeg 파일을 PictureContainer로 변환 하는 함수
//    fun createPictureContainer(activity: Activity, MCContainer:MCContainer, sourceByteArray: ByteArray) {
//        var header : Header = Header()
//
//        // start header parsing ...
//        var pictureInfoList : ArrayList<PictureInfo>? = arrayListOf()
//        val intList = mutableListOf<Int>()
//        //몇 개의 사진이 들어있는지 알아내기
//        // APP3의 마커의 인덱스 위치를 2로 고정 시켜서 데이터의 시작은 4 인덱스 부터로 확신하여 짠 코드
//        // 후에 App3 마커를 찾은 후부터 데이터를 parisng하도록 고쳐야함!
//        var count :Int = ((sourceByteArray[4].toInt() and 0xFF) shl 24) or
//                ((sourceByteArray[5].toInt() and 0xFF) shl 16) or
//                ((sourceByteArray[6].toInt() and 0xFF) shl 8) or
//                ((sourceByteArray[7].toInt() and 0xFF))
//        //기록된 헤더 데이터를 4 바이트씩 끊어서 intList에 저장
//        for (i in 8 until (header.INFO_SIZE)*count+8 step 4) {
//            // ByteArray의 4바이트를 읽어서 Int로 변환하여 List에 추가
//            intList.add(
//                ((sourceByteArray[i].toInt() and 0xFF) shl 24) or
//                        ((sourceByteArray[i + 1].toInt() and 0xFF) shl 16) or
//                        ((sourceByteArray[i + 2].toInt() and 0xFF) shl 8) or
//                        ((sourceByteArray[i + 3].toInt() and 0xFF))
//            )
//        }
//        // picturInfo 생성
//        for(i in 0..intList.size-1 step 4){
//            var groupID =intList.get(i)
//            var typeCode = intList.get(i+1)
//            var offset =  intList.get(i+2)
//            var size = intList.get(i+3)
//            var picturInfo = PictureInfo(groupID,typeCode,offset,size)
//            pictureInfoList?.add(picturInfo)
//
//        }
//        // 헤더 생성
//        var newHeader = pictureInfoList?.let { Header(it) }
//        if (newHeader != null) {
//            MCContainer.setHeader(newHeader)
//        }
//        // end header parsing ...
//
//        //start PictureList parsing  ...
//        CoroutineScope(Dispatchers.IO).launch {
//            var pictureList : ArrayList<Picture> = arrayListOf()
//            var startIndex = 0
//            var endIndex = 0
//            for(i in 0..count-1){
//                var pictureInfor = pictureInfoList?.get(i)
//                startIndex = pictureInfor?.offset!!
//                endIndex = startIndex + pictureInfor?.size!!-1
//                var length = endIndex - startIndex + 1
//                val targetImageArray = ByteArray(length)
//                // sliceArray() 함수를 사용하여 잘라낸 ByteArray를 생성
//                sourceByteArray.sliceArray(startIndex..endIndex).copyInto(targetImageArray)
//                // type 정보
//                var picture : Picture = Picture(targetImageArray, ImageType.basic)
//                pictureList.add(picture)
//            }
//            MCContainer.setPictureList(pictureList)
//            // end PictureList parsing ...
//            MCContainer.setCount(count)
//        }

  // }
}